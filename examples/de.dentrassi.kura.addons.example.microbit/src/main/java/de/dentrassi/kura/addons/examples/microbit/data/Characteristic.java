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
package de.dentrassi.kura.addons.examples.microbit.data;

import static de.dentrassi.kura.addons.examples.microbit.data.UuidParser.parse;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.kura.message.KuraPayload;

public class Characteristic {

    private final java.util.UUID id;
    private final BiConsumer<KuraPayload, ByteBuffer> consumer;
    private final Consumer<KuraPayload> whenMissing;

    public Characteristic(final String id, final BiConsumer<KuraPayload, ByteBuffer> consumer, final Consumer<KuraPayload> whenMissing) {
        this.id = parse(id);
        this.consumer = consumer;
        this.whenMissing = whenMissing;
    }

    public Characteristic(final String id, final BiConsumer<KuraPayload, ByteBuffer> consumer) {
        this(id, consumer, ignore -> {
        });
    }

    public java.util.UUID getId() {
        return this.id;
    }

    public void handle(final KuraPayload payload, final byte[] data) {
        if (data != null) {
            this.consumer.accept(payload, ByteBuffer.wrap(data));
        } else {
            this.whenMissing.accept(payload);
        }
    }
}
