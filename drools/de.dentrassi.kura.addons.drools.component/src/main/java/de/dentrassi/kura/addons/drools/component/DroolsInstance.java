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

package de.dentrassi.kura.addons.drools.component;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import org.drools.core.io.impl.ReaderResource;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import de.dentrassi.kura.addons.drools.Configuration;
import de.dentrassi.kura.addons.drools.Drools;

@Component(immediate = true, configurationPolicy = REQUIRE)
public class DroolsInstance implements ConfigurableComponent {

    private Drools drools;
    private BundleContext context;
    private KieSession session;
    private ServiceRegistration<KieSession> registration;

    @Reference
    public void setDrools(final Drools drools) {
        this.drools = drools;
    }

    @Activate
    public void activate(final BundleContext context, final Map<String, Object> properties) {

        this.context = context;
        if (!Configuration.asBoolean(properties, "enabled")) {
            return;
        }

        final String id = Configuration.asString(properties, "id", UUID.randomUUID().toString());
        final String eventProcessingOptionString = Configuration.asString(properties, "eventProcessingOption");
        final String rules = Configuration.asString(properties, "rules");
        final String type = Configuration.asString(properties, "type", "DRL");

        final ReaderResource resource;

        if (rules != null) {
            resource = new ReaderResource(new StringReader(rules));
        } else {
            resource = null;
        }

        final ResourceType resourceType = ResourceType.getResourceType(type);

        final EventProcessingOption eventProcessingOption;
        if (eventProcessingOptionString != null) {
            eventProcessingOption = EventProcessingOption.determineEventProcessingMode(eventProcessingOptionString);
        } else {
            eventProcessingOption = null;
        }

        this.session = this.drools

                .newKnowledgeBuilderBaseBuilder(context)

                .configureBase(builder -> {
                    if (eventProcessingOption != null) {
                        builder.setOption(eventProcessingOption);
                    }
                })

                .customize(builder -> {

                    if (resource != null) {
                        builder.add(resource, resourceType);
                    }

                })
                .build()
                .newKieSession();

        setupGlobals(this.session);

        final Hashtable<String, Object> serviceProperties = new Hashtable<>();

        serviceProperties.put(Constants.SERVICE_PID, properties.get(Constants.SERVICE_PID));
        serviceProperties.put("drools.session.id", id);
        this.registration = context.registerService(KieSession.class, this.session, serviceProperties);

    }

    protected void setupGlobals(final KieSession session) {
    }

    @Deactivate
    public void deactivate() {

        if (this.registration != null) {
            this.registration.unregister();
            this.registration = null;
        }
        if (this.session != null) {
            this.session.dispose();
            this.session = null;
        }

    }

    @Modified
    public void modified(final Map<String, Object> properties) {

        deactivate();
        activate(this.context, properties);

    }
}
