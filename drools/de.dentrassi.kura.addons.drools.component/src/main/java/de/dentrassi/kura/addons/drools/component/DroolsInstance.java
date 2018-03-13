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

import static de.dentrassi.kura.addons.drools.Configuration.asOptionalString;
import static java.util.Collections.singletonList;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;

import org.drools.core.io.impl.ReaderResource;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.conf.SequentialAgendaOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dentrassi.kura.addons.drools.Configuration;
import de.dentrassi.kura.addons.drools.Drools;

@Component(immediate = true, configurationPolicy = REQUIRE)
public class DroolsInstance implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(DroolsInstance.class);

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

        try {
            final String id = Configuration.asString(properties, "id", UUID.randomUUID().toString());

            final Optional<EventProcessingOption> eventProcessingOption = asOptionalString(properties,
                    "eventProcessingOption")
                            .map(EventProcessingOption::determineEventProcessingMode);

            final String rules = Configuration.asString(properties, "rules");
            final String type = Configuration.asString(properties, "type", "DRL");

            final Optional<SequentialAgendaOption> sequentialAgendaOption = asOptionalString(properties,
                    "sequentialAgendaOption")
                            .map(SequentialAgendaOption::valueOf);

            final boolean fireUntilHalt = Configuration.asBoolean(properties, "fireUntilHalt");

            final Map<ResourceType, List<String>> resources = new HashMap<>();

            if ("COMPOSITE".equals(type)) {
                readComposite(resources, rules);
            } else {
                final ResourceType resourceType = ResourceType.getResourceType(type);
                resources.put(resourceType, singletonList(rules != null ? rules : ""));
            }

            this.session = this.drools

                    .newKnowledgeBuilderBaseBuilder(context)

                    .configureBase(config -> {

                        eventProcessingOption.ifPresent(config::setOption);
                        sequentialAgendaOption.ifPresent(config::setOption);

                    })

                    .customize(builder -> {

                        for (final Map.Entry<ResourceType, List<String>> entry : resources.entrySet()) {
                            for (final String s : entry.getValue()) {
                                builder.add(new ReaderResource(new StringReader(s)), entry.getKey());
                            }
                        }

                    })

                    .build()
                    .newKieSession();

            setupGlobals(this.session);

            final Hashtable<String, Object> serviceProperties = new Hashtable<>();

            serviceProperties.put(Constants.SERVICE_PID, properties.get(Constants.SERVICE_PID));
            serviceProperties.put("drools.session.id", id);
            this.registration = context.registerService(KieSession.class, this.session, serviceProperties);

            if (fireUntilHalt) {
                final Thread t = new Thread(this.session::fireUntilHalt);
                t.setContextClassLoader(DroolsInstance.class.getClassLoader());
                t.setName("DroolsInstance-fireUntilHalt-" + id);
                t.start();
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(final Thread t, final Throwable e) {
                        logger.error("Drools thread failed: " + t.getName(), e);
                    }
                });
            }
        } catch (final Exception e) {
            /*
             * We need to catch all exception here and not throw anything. Otherwise Kura
             * will simply drop the service and we will never be able to fix the
             * configuration again. This would mean wiping the device.
             */
            logger.error("Failed to configure drools session", e);
            e.printStackTrace(); // yes, we really do that, so that we can read it from the console
        }
    }

    private void readComposite(final Map<ResourceType, List<String>> resources, final String rules)
            throws Exception {

        if (rules == null) {
            return;
        }

        final MimeMultipart mm = new MimeMultipart(new DataSource() {

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(rules.getBytes());
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getContentType() {
                return "text/plain";
            }

            @Override
            public String getName() {
                return null;
            }
        });

        for (int i = 0; i < mm.getCount(); i++) {
            final BodyPart body = mm.getBodyPart(i);

            final String[] typeHeader = body.getHeader("Drools-Resource-Type");
            if (typeHeader == null || typeHeader.length <= 0 || typeHeader[0] == null) {
                throw new IllegalArgumentException("'Drools-Resource-Type' header missing");
            }

            final String resourceTypeName = typeHeader[0];
            final ResourceType resourceType = ResourceType.getResourceType(resourceTypeName);
            final String content = body.getContent().toString();

            resources
                    .computeIfAbsent(resourceType, k -> new LinkedList<>())
                    .add(content);
        }

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
            this.session.halt();
            this.session.dispose();
            this.session = null;
        }

    }

    @Modified
    public void modified(final Map<String, Object> properties) throws Exception {

        deactivate();
        activate(this.context, properties);

    }
}
