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

package de.dentrassi.kura.addons.drools.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kie.api.internal.utils.ServiceDiscoveryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dentrassi.kura.addons.drools.Drools;

@Component
public class ServiceActivator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceActivator.class);

    private static final Collection<?> REQUIRED_CLASSES = new HashSet<>(
            Arrays.asList("org.kie.internal.builder.KnowledgeBuilderFactoryService", "org.kie.api.KieServices"));

    private DroolsImpl drools = new DroolsImpl();

    private ServiceDiscoveryImpl serviceDiscovery;

    private BundleContext context;

    private ServiceRegistration<Drools> registration;

    private ScheduledExecutorService executors;

    private ScheduledFuture<?> job;

    @Reference
    public void setServiceDiscovery(final ServiceDiscoveryImpl serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Activate
    protected void activate(final BundleContext context) {
        this.context = context;
        this.executors = Executors.newScheduledThreadPool(1);
        this.job = this.executors.scheduleAtFixedRate(this::checkAndPublish, 0, 1, TimeUnit.SECONDS);
    }

    @Deactivate
    protected void deactivate() {
        shutdownChecker();
        publish(false);
    }

    private void shutdownChecker() {
        if (this.job != null) {
            this.job.cancel(false);
            this.job = null;
        }
        if (this.executors != null) {
            this.executors.shutdown();
            this.executors = null;
        }
    }

    protected void checkAndPublish() {
        publish(isReady());
    }

    protected boolean isReady() {

        final ServiceDiscoveryImpl serviceDiscovery = this.serviceDiscovery;

        if (serviceDiscovery == null) {
            return false;
        }

        serviceDiscovery.reset();

        final Map<String, Object> services = serviceDiscovery.getServices();

        if (!services.keySet().containsAll(REQUIRED_CLASSES)) {
            return false;
        }

        return true;
    }

    protected void publish(final boolean state) {
        if (state) {
            if (registration == null) {

                if (logger.isInfoEnabled()) {
                    logger.info("Current Drools registry:");
                    for (final String name : serviceDiscovery.getServices().keySet()) {
                        logger.info("\t{}", name);
                    }
                }

                final Hashtable<String, Object> properties = new Hashtable<>();
                properties.put(Constants.SERVICE_DESCRIPTION, "Drools helper interface");
                this.registration = this.context.registerService(Drools.class, this.drools, properties);

                shutdownChecker();
            }
        } else {
            if (this.registration != null) {
                this.registration.unregister();
                this.registration = null;
            }
        }
    }
}
