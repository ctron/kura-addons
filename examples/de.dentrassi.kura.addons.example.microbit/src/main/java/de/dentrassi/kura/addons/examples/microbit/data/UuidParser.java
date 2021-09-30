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

import java.util.UUID;

public final class UuidParser {

    private UuidParser() {
    }

    public static UUID parse(final String id) {
        final var sb = new StringBuilder(id);

        sb.insert(8 + 4 + 4 + 4, '-');
        sb.insert(8 + 4 + 4, '-');
        sb.insert(8 + 4, '-');
        sb.insert(8, '-');

        return UUID.fromString(sb.toString());
    }

}
