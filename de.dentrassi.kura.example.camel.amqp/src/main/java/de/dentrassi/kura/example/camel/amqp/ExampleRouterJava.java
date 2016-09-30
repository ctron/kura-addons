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

package de.dentrassi.kura.example.camel.amqp;

import static org.eclipse.kura.camel.component.Configuration.asString;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.amqp.AMQPComponent;
import org.eclipse.kura.camel.runner.BeforeStart;
import org.eclipse.kura.camel.runner.CamelRunner;
import org.eclipse.kura.camel.runner.CamelRunner.Builder;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example for using AMQP
 */
public class ExampleRouterJava implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ExampleRouterJava.class);

    private String brokerUrl;

    private CamelRunner camelRunner;

    public void start(final BundleContext context, final Map<String, Object> properties) throws Exception {
        logger.info("Starting");

        // apply configuration

        applyMajorChange(properties);

        // start router

        this.camelRunner = createRunner();
        setRoutesFromProperties(properties);
        this.camelRunner.start();
    }

    public void updated(final Map<String, Object> properties) throws Exception {
        logger.info("Updating");

        // apply configuraton

        if (applyMajorChange(properties)) {

            // require restart

            this.camelRunner.stop();
            this.camelRunner = createRunner();
            setRoutesFromProperties(properties);
            this.camelRunner.start();

        } else {

            // apply on the fly

            setRoutesFromProperties(properties);

        }
    }

    private CamelRunner createRunner() {

        // create a new router builder

        final Builder builder = new CamelRunner.Builder();

        // register AMQP

        builder.addBeforeStart(new BeforeStart() {
            @Override
            public void beforeStart(CamelContext camelContext) throws Exception {
                registerAmqp(camelContext);
            }
        });

        // build runner

        return builder.build();
    }

    private void registerAmqp(CamelContext camelContext) {

        // Register AMQP component current broker URL

        final AMQPComponent amqp = AMQPComponent.amqpComponent(this.brokerUrl);
        camelContext.addComponent("amqp", amqp);
    }

    /**
     * Apply major change configuration
     * <p>
     * This method selectively applies configuration which does request a Camel router
     * restart.
     * </p>
     * 
     * @param properties
     *            The properties to apply
     * @return {@code true} if a router restart is necessary, {@code false} otherwise
     */
    private boolean applyMajorChange(Map<String, Object> properties) {
        boolean changed = false;

        final String newBrokerUrl = asString(properties, "brokerUrl", "amqp://localhost:5432");
        if (this.brokerUrl == null || !this.brokerUrl.equals(newBrokerUrl)) {
            this.brokerUrl = newBrokerUrl;
            changed = true;
        }

        return changed;
    }

    /**
     * Apply Camel routes from configuration properties
     * 
     * @param properties
     *            the properties to apply
     * @throws Exception
     *             if anything goes wrong
     */
    private void setRoutesFromProperties(Map<String, Object> properties) throws Exception {
        this.camelRunner.setRoutes(buildRoutes(properties));
    }

    /**
     * Create a new instance of {@link RouteBuilder} which reflects the current state of the component
     * <p>
     * Please not that each instance of {@link RouteBuilder} will only build the routes once. So re-applying
     * the same instance of {@link RouteBuilder} will not trigger a second call to {@link RouteBuilder#configure()}
     * </p>
     * 
     * @param properties
     *            the configuration properties to build the routes from
     * @return the routes builder instance
     */
    private RouteBuilder buildRoutes(final Map<String, Object> properties) {
        final String topic = asString(properties, "topic", "test");

        logger.info("Properties loaded - topic: {}", topic);

        // always return a new instance of RouteBuilder
        
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer:test").setBody().simple("foo").to("amqp:topic://" + topic);
            }
        };
    }
}