/*
 * Copyright (C) 2016 Jens Reimann <jreimann@redhat.com>
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

package de.dentrassi.kura.artemis.server;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPolicy = REQUIRE, enabled = true, immediate = true, service = ConfigurableComponent.class)
public class ServiceComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ServiceComponent.class);

    private ServerConfiguration configuration;
    private ServerManager server;

    @Activate
    public void activate(final Map<String, Object> properties) throws Exception {
        final ServerConfiguration cfg = parse(properties);
        if (cfg != null) {
            start(cfg);
        }
    }

    @Modified
    public void modified(final Map<String, Object> properties) throws Exception {
        final ServerConfiguration cfg = parse(properties);
        if (this.configuration == cfg) {
            logger.debug("Configuration identical .... skipping update");
            return;
        }
        if (this.configuration != null && this.configuration.equals(cfg)) {
            logger.debug("Configuration equal .... skipping update");
            return;
        }

        stop();
        if (cfg != null) {
            start(cfg);
        }
    }

    @Deactivate
    public void deactivate() throws Exception {
        stop();
    }

    private void start(final ServerConfiguration configuration) throws Exception {
        logger.info("Starting Artemis");

        this.server = new ServerManager(configuration);
        this.server.start();

        this.configuration = configuration;
    }

    private void stop() throws Exception {
        logger.info("Stopping Artemis");

        if (this.server != null) {
            this.server.stop();
            this.server = null;
        }

        this.configuration = null;
    }

    private ServerConfiguration parse(final Map<String, Object> properties) {

        // parse broker XML

        final String brokerXml = (String) properties.get("brokerXml");
        if (brokerXml == null || brokerXml.isEmpty()) {
            return null;
        }

        // parse required protocols

        final Set<String> requiredProtocols = new HashSet<>();
        {
            final Object v = properties.get("requiredProtocols");
            if (v instanceof String[]) {
                requiredProtocols.addAll(Arrays.asList((String[]) v));
            } else if (v instanceof String) {
                final String vs = (String) v;
                final String[] reqs = vs.split("\\s*,\\s*");
                requiredProtocols.addAll(Arrays.asList(reqs));
            }
        }

        // create security configuration

        final SecurityConfiguration securityConfiguration = new SecurityConfiguration();

        final String defaultUser = (String) properties.get("defaultUser");
        if (defaultUser != null) {
            securityConfiguration.setDefaultUser(defaultUser);
        }

        {
            final String users = (String) properties.get("users");
            if (users != null) {
                final Properties p = new Properties();
                try {
                    p.load(new StringReader(users));
                } catch (final IOException e) {
                    logger.info("Failed to parse users", e);
                }

                for (final String key : p.stringPropertyNames()) {
                    final String value = p.getProperty(key);
                    final String[] toks = value.split("\\|", 2);
                    if (toks.length == 2) {
                        final String password = toks[0];
                        securityConfiguration.addUser(key, password);

                        final String[] roles = toks[1].split("\\s*,\\*s");
                        for (final String role : roles) {
                            securityConfiguration.addRole(key, role);
                        }
                    }
                }
            }
        }

        // create result

        final ServerConfiguration cfg = new ServerConfiguration();
        cfg.setBrokerXml(brokerXml);
        cfg.setRequiredProtocols(requiredProtocols);
        cfg.setSecurityConfiguration(securityConfiguration);
        return cfg;
    }

}
