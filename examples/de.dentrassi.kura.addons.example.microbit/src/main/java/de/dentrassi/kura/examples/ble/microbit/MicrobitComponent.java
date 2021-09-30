/*
 * Copyright (c) 2021 Red Hat Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 */
package de.dentrassi.kura.examples.ble.microbit;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import de.dentrassi.kura.examples.ble.microbit.data.Characteristic;
import de.dentrassi.kura.examples.ble.microbit.data.Service;

/**
 * A micro:bit BLE example.
 *
 * @author Jens Reimann
 */
@Designate(ocd = Config.class)
@Component(immediate = true, configurationPolicy = REQUIRE)
public class MicrobitComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(MicrobitComponent.class);

    private ScheduledExecutorService executor;

    private Config config;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, bind = "setCloudPublisher")
    private CloudPublisher cloudPublisher;

    protected void setCloudPublisher(final CloudPublisher cloudPublisher) {
        logger.info("Setting cloud publisher: {}", cloudPublisher);
        this.cloudPublisher = cloudPublisher;
    }

    @Reference
    private BluetoothLeService bluetooth;

    private BluetoothLeAdapter adapter;
    private List<BluetoothLeDevice> devices = Collections.emptyList();

    @Activate
    public void activate(final Config config) {
        dumpProperties("activate", config);
        setConfig(config);
    }

    @Modified
    public void modified(final Config config) {
        dumpProperties("modified", config);
        setConfig(config);
    }

    @Deactivate
    public void deactivate() {
        logger.info("Deactivate");
        stop();
    }

    private void tick() {

        logger.info("Ticking ...");

        final var cloudPublisher = this.cloudPublisher;
        try {
            if (cloudPublisher != null) {
                cloudPublisher.publish(makeMessage());
            }
        } catch (final Exception e) {
            logger.warn("Failed to publish", e);
        }

    }

    protected static KuraMessage makeMessage() {
        return new KuraMessage(makePayload());
    }

    protected static KuraPayload makePayload() {
        final KuraPayload payload = new KuraPayload();
        payload.addMetric("value", "foo");
        return payload;
    }

    private boolean needRestart(final Config config) {
        if (this.config == null) {
            return false;
        }

        if (this.config.interfaceName().equals(config.interfaceName())) {
            return true;
        }

        return false;
    }

    private void setConfig(final Config config) {

        if (!config.enabled()) {
            logger.info("Component is not enabled");
            stop();
            return;
        }

        if (needRestart(config)) {
            logger.info("Configuration changes require a restart");
            stop();
        }

        this.config = config;

        // init

        logger.info("Starting ...");
        start();
        logger.info("Starting ... done!");
    }

    private void refreshDevices() {
        logger.info("Refresh devices");
        this.devices = performScan();
    }

    private List<BluetoothLeDevice> performScan() {

        final var result = new LinkedList<BluetoothLeDevice>();

        final var adapter = this.adapter;
        if (adapter == null) {
            logger.info("No bluetooth adapter found -> no devices");
            return result;
        }

        logger.info("Adapter - name: {}, alias: {}, address: {}, interfaceName: {}, powered: {}, discovering: {}",
                adapter.getName(),
                adapter.getAlias(),
                adapter.getAddress(),
                adapter.getInterfaceName(),
                adapter.isPowered(),
                adapter.isDiscovering()
        );

        try {
            if (adapter.isDiscovering()) {
                logger.info("Stopping active discovery");
                adapter.stopDiscovery();
            }

            logger.info("Begin scan ({} seconds) ...", this.config.scanTime());
            for (final var device : adapter.findDevices(this.config.scanTime()).get()) {
                final var name = device.getName();
                logger.info("Device - Name: {}, Address: {}, Alias: {}, Icon: {}", name, device.getAddress(), device.getAlias(), device.getIcon());
                logger.info("\tUUIDs: {}", (Object) device.getUUIDs());
                logger.info("\tManufacturer Data: {}", device.getManufacturerData());
                logger.info("\tService Data: {}", device.getServiceData());
                if (name != null && name.startsWith("BBC micro:bit")) {
                    logger.info("Adding device: {}", name);
                    result.add(device);
                }
            }

            logger.info("Scan complete");
        } catch (final Exception e) {
            logger.warn("Failed to scan for devices", e);
        }

        return result;
    }

    private void refreshData() {
        logger.info("Refresh data...");

        final var devices = this.devices;
        for (final var device : devices) {
            try {
                refreshDevice(device);
            } catch (final Exception e) {
                logger.info("Failed to refresh device data", e);
                try {
                    device.disconnect();
                    logger.info("Disconnected from device");
                } catch (final Exception e2) {
                    logger.info("Failed to clean up after error", e2);
                }
                try {
                    device.cancelPairing();
                } catch (final Exception e2) {
                    logger.info("Failed to clean up after error", e2);
                }
            }
        }

        logger.info("Refresh data... done!");
    }

    private void refreshDevice(final BluetoothLeDevice device) throws Exception {
        logger.info("Refresh device: {} - connected: {}, paired: {}, legacyPairing: {}, blocked: {}, trusted: {}, resolved: {}",
                device.getName(),
                device.isConnected(),
                device.isPaired(),
                device.isLegacyPairing(),
                device.isBlocked(),
                device.isTrusted(),
                device.isServicesResolved()
        );

        /*
        if (!device.isPaired()) {
            logger.info("Pairing ...");
            device.pair();
            logger.info("Pairing ... done!");
        }
        */

        if (!device.isConnected()) {
            logger.info("Connecting ...");
            device.connect();
            logger.info("Connecting ... done!");
        }

        if (this.config.introspect()) {
            introspect(device);
        }

        // acquire data

        final var payload = new KuraPayload();
        final var errors = new HashMap<String, String>();
        final var missing = new HashSet<String>();
        for (final var service : Service.values()) {
            logger.info("\tProcessing {} - {}", service.name(), service.getId());
            try {
                final var s = device.findService(service.getId());
                if (s != null) {
                    for (final var ch : service.getCharacteristics()) {
                        try {
                            readValue(s, ch, payload);
                        } catch (final Exception e) {
                            errors.put(String.format("%s/%s", service.getId(), ch.getId()), e.getMessage());
                        }
                    }
                } else {
                    missing.add(service.getId().toString());
                }
            } catch (final Exception e) {
                errors.put(service.getId().toString(), e.getMessage());
            }
        }

        // add meta information

        final var body = new LinkedHashMap<String, Object>();
        if (!errors.isEmpty()) {
            body.put("errors", errors);
        }
        if (!missing.isEmpty()) {
            body.put("missing", missing);
        }

        logger.info("Metrics: {}", new GsonBuilder().setPrettyPrinting().create().toJson(payload));
        logger.info("Body: {}", new GsonBuilder().setPrettyPrinting().create().toJson(body));

        // add the payload afterwards

        payload.setBody(new GsonBuilder().create().toJson(body).getBytes(StandardCharsets.UTF_8));

        // publish data

        final var properties = new HashMap<String, Object>();
        properties.put("assetName", device.getAddress());
        logger.info("Properties: {}", properties);
        final var message = new KuraMessage(payload, properties);

        final var publisher = this.cloudPublisher;
        if (publisher != null) {
            publisher.publish(message);
        } else {
            logger.info("Skipping publish due to missing publisher");
        }
    }

    private static void readValue(final BluetoothLeGattService s, final Characteristic ch, final KuraPayload payload) throws KuraBluetoothIOException {
        logger.info("\t\tFinding: {}", ch.getId());
        try {
            final var c = s.findCharacteristic(ch.getId());

            final byte[] v;
            if (c != null) {
                logger.info("\t\tReading: {}", ch.getId());
                v = c.readValue();
            } else {
                v = null;
            }
            ch.handle(payload, v);

        } catch (final KuraBluetoothResourceNotFoundException e) {
            ch.handle(payload, null);
        }
    }

    private void introspect(final BluetoothLeDevice device) throws Exception {

        logger.info("Finding services");

        final var services = device.findServices();

        logger.info("Found: {}", services.size());

        for (final var service : services) {

            logger.info("\tService - {} ({})", service.getUUID(), service.isPrimary());
            final var chars = service.findCharacteristics();
            for (final var c : chars) {
                byte[] value = null;
                try {
                    value = c.readValue();
                } catch (final Exception e) {
                    logger.info("Failed to read value", e);
                }
                logger.info("\t\tCharacteristic: {} - {}", c.getUUID(), value);
            }

        }
    }

    private void start() {

        if (this.adapter == null) {
            this.adapter = this.bluetooth.getAdapter(this.config.interfaceName());
        }

        logger.info("Bluetooth adapter ({}) = {}", this.config.interfaceName(), this.adapter);

        if (this.adapter != null) {
            logger.info("\tPowered = {}", this.adapter.isPowered());

            if (!this.adapter.isPowered()) {
                logger.info("Activate bluetooth adapter...");
                this.adapter.setPowered(true);
            }
        }

        if (this.executor == null) {
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.scheduleWithFixedDelay(this::refreshDevices, 5, 60, TimeUnit.SECONDS);
            this.executor.scheduleWithFixedDelay(this::refreshData, 20, 10, TimeUnit.SECONDS);
        }

    }

    private void stop() {
        logger.info("Stopping ...");
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }
        this.devices = Collections.emptyList();
        this.adapter = null;
    }

    private void dumpProperties(final String operation, final Config config) {
        if (logger.isInfoEnabled()) {
            logger.info("=========== {} ===========", operation);
            logger.info("\t@bluetooth = {}", this.bluetooth);
            logger.info("\t'enabled' = '{}'", config.enabled());
            logger.info("=========== {} ===========", operation);
        }
    }
}
