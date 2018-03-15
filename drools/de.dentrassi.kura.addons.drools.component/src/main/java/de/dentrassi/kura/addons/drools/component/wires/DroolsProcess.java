/*
 * Copyright (C) 2018 Jens Reimann <jreimann@redhat.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dentrassi.kura.addons.drools.component.wires;

import static de.dentrassi.kura.addons.drools.component.wires.Wires.toMap;
import static de.dentrassi.kura.addons.drools.component.wires.Wires.toRecords;
import static org.eclipse.kura.type.TypedValues.newTypedValue;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.api.runtime.rule.FactHandle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.wireadmin.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dentrassi.kura.addons.drools.Configuration;

@Component(enabled = true, configurationPolicy = REQUIRE, service = { ConfigurableComponent.class, WireComponent.class,
        WireReceiver.class, org.osgi.service.wireadmin.Consumer.class, WireEmitter.class, Producer.class }, property = {
                "service.pid=de.dentrassi.kura.addons.drools.component.wires.DroolsProcess" })
public class DroolsProcess extends AbstractDroolsWireComponent implements WireReceiver, WireEmitter {

    private static final Logger logger = LoggerFactory.getLogger(DroolsProcess.class);

    private String entryPoint;
    private boolean fireAllRules;
    private boolean delete;

    private String factPackage;
    private String factType;

    private Map<String, String> inputs;
    private Map<String, String> outputs;

    @Override
    @Reference
    public void setWireHelperService(final WireHelperService wireHelperService) {
        super.setWireHelperService(wireHelperService);
    }

    @Override
    @Activate
    protected void activate(final Map<String, ?> properties) throws Exception {
        this.entryPoint = Configuration.asString(properties, "entryPoint");
        this.fireAllRules = Configuration.asBoolean(properties, "fireAllRules", true);
        this.delete = Configuration.asBoolean(properties, "delete", true);

        this.factPackage = Configuration.asString(properties, "factPackage");
        this.factType = Configuration.asString(properties, "factType");

        this.inputs = Configuration.asOptionalString(properties, "inputs")
                .map(DroolsProcess::parseMappings)
                .orElse(null);

        this.outputs = Configuration.asOptionalString(properties, "outputs")
                .map(DroolsProcess::parseMappings)
                .orElse(null);

        if (this.factPackage == null || this.factType == null || this.inputs == null || this.outputs == null) {
            return;
        }

        super.activate(properties);
    }

    @Override
    @Modified
    protected void modified(final Map<String, ?> properties) throws Exception {
        super.modified(properties);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    private static Map<String, String> parseMappings(final String input) {
        final Map<String, String> result = new HashMap<>();
        for (final String pair : input.split("\\s*[,;]\\s*")) {
            final String[] toks = pair.split("\\=", 2);
            if (toks.length == 2) {
                result.put(toks[0], toks[1]);
            }
        }
        return result;
    }

    @Override
    public void onWireReceive(final WireEnvelope envelope) {
        withSession(session -> {
            try {
                processReceive(envelope, session);
            } catch (final Exception e) {
                logger.warn("Failed to process", e);
            }
        });
    }

    private void processReceive(final WireEnvelope envelope, final KieSession session) {
        EntryPoint ep = session;
        if (this.entryPoint != null && !this.entryPoint.isEmpty()) {
            ep = session.getEntryPoint(this.entryPoint);
        }

        if (ep == null) {
            return;
        }

        final FactType factType = session.getKieBase().getFactType(this.factPackage, this.factType);

        if (factType == null) {
            logger.warn("Unable to find fact type: {}:{}", this.factPackage, factType);
            return;
        }

        final Object fact;
        try {
            fact = factType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to created fact instance: " + this.factPackage + ":" + this.factType, e);
        }

        final Map<String, TypedValue<?>> values = toMap(envelope);

        for (final Map.Entry<String, String> entry : this.inputs.entrySet()) {
            final String target = entry.getKey();
            final String source = entry.getValue();

            final TypedValue<?> value = values.get(source);
            if (value == null) {
                logger.warn("Missing value: {}", source);
                throw new IllegalStateException();
            }
            factType.set(fact, target, value.getValue());
        }

        final FactHandle handle = ep.insert(fact);

        if (this.fireAllRules) {
            session.fireAllRules();
        }

        final Map<String, TypedValue<?>> result = new HashMap<>(this.outputs.size());

        for (final Map.Entry<String, String> entry : this.outputs.entrySet()) {
            final String target = entry.getKey();
            final String source = entry.getValue();

            final Object value = factType.get(fact, source);

            if (logger.isInfoEnabled()) {
                logger.info("Result - type: {}, value: {}", value != null ? value.getClass() : "<null>", value);
            }

            if (value != null) {
                result.put(target, newTypedValue(value));
            }
        }

        if (this.delete) {
            ep.delete(handle);
        }

        this.wireSupport.emit(toRecords(result));
    }

}
