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
package de.dentrassi.kura.examples.ble.microbit.data;

import static de.dentrassi.kura.examples.ble.microbit.data.UuidParser.parse;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import org.eclipse.kura.message.KuraPayload;

public enum Service {
    DEVICE_INFORMATION("0000180A00001000800000805F9B34FB", new Characteristic[]{
            new Characteristic("00002A2400001000800000805F9B34FB", stringValue("modelNumber")),
            new Characteristic("00002A2500001000800000805F9B34FB", stringValue("serialNumber")),
            new Characteristic("00002A2700001000800000805F9B34FB", stringValue("hardwareRevision")),
            new Characteristic("00002A2900001000800000805F9B34FB", stringValue("manufacturerName")),
    }),
    TEMPERATURE("E95D6100251D470AA062FA1922DFA9A8", new Characteristic[]{
            new Characteristic("E95D9250251D470AA062FA1922DFA9A8", (payload, data) -> {
                payload.addMetric("temperature", (int) data.get());
            })
    });

    private final Characteristic[] characteristics;
    private final java.util.UUID id;

    Service(final String id, final Characteristic[] characteristics) {
        this.id = parse(id);
        this.characteristics = characteristics;
    }

    public java.util.UUID getId() {
        return this.id;
    }

    public Characteristic[] getCharacteristics() {
        return this.characteristics;
    }

    public static BiConsumer<KuraPayload, ByteBuffer> stringValue(final String metricName) {
        return (payload, data) -> {
            try {
                payload.addMetric(metricName, StandardCharsets.UTF_8.newDecoder().decode(data).toString());
            } catch (final CharacterCodingException ignored) {
            }
        };
    }
}
