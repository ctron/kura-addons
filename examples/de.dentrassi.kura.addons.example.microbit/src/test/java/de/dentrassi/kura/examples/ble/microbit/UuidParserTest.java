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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.dentrassi.kura.examples.ble.microbit.data.UuidParser;

public class UuidParserTest {

    @Test
    public void testParse() {
        Assertions.assertEquals(java.util.UUID.fromString("E95D6100-251D-470A-A062-FA1922DFA9A8"), UuidParser.parse("E95D6100251D470AA062FA1922DFA9A8"));
    }

}