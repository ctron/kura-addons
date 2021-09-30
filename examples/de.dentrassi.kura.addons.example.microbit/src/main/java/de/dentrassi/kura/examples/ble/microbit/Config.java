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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Meta type information for {@link MicrobitComponent}
 */
@ObjectClassDefinition(
        id = "de.dentrassi.kura.examples.ble.microbit.MicrobitComponent",
        name = "micro:bit",
        description = "Micro:bit example component",
        icon = {
                @Icon(resource = "microbit.png", size = 14)
        }
)
@interface Config {

    @AttributeDefinition(
            name = "CloudPublisher Target Filter",
            description = "Specifies, as an OSGi target filter, the pid of the Cloud Publisher used to publish messages to the cloud platform."
    )
    String cloudPublisher_target() default "(kura.service.pid=changeme)";

    @AttributeDefinition(
            name = "Enabled",
            description = "Whether the component is enabled or not."
    )
    boolean enabled() default false;

    @AttributeDefinition(
            name = "Bluetooth Interface Name",
            description = "The device name of the Bluetooth adapter."
    )
    String interfaceName() default "hci0";

    @AttributeDefinition(
            name = "Scan time (seconds)",
            description = "The amount of time the service will scan for Bluetooth devices."
    )
    long scanTime() default 10;

    @AttributeDefinition(
            name = "Introspect to log",
            description = "This scan all services and characteristics from a device and write it to the log. It can be used to debug data."
    )
    boolean introspect() default false;
}
