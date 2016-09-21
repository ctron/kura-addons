/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package de.dentrassi.kura.example.camel.amqp;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.amqp.AMQPComponent;
import org.eclipse.kura.camel.router.AbstractCamelRouter;
import org.eclipse.kura.configuration.ConfigurableComponent;

/**
 * Example for using AMQP
 */
public class ExampleRouterJava extends AbstractCamelRouter implements ConfigurableComponent {

    private String brokerUrl;
    private String topic;

    public ExampleRouterJava() {
        // load defaults
        loadProperties(null);
    }

    public void updated(Map<String, Object> properties) throws Exception {
        stop();
        loadProperties(properties);
        start();
    }

    private void loadProperties(Map<String, Object> properties) {
        this.brokerUrl = asString(properties, "brokerUrl", "amqp://localhost:5672");
        this.topic = asString(properties, "topic", "test");
    }

    @Override
    protected void registerFeatures(CamelContext camelContext) {
        final AMQPComponent amqp = AMQPComponent.amqpComponent(brokerUrl);
        camelContext.addComponent("amqp", amqp);
    }

    @Override
    public void configure() {
        from("timer:test").setBody().simple("foo").to("amqp:topic://" + topic);
    }

    private String asString(final Map<String, Object> properties, final String key, final String defaultValue) {
        final Object value = properties != null ? properties.get(key) : null;

        if (value instanceof String) {
            return (String) value;
        }

        return defaultValue;
    }

}