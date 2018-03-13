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

package de.dentrassi.kura.addons.drools.component.wires.internal;

import java.util.List;

import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;

import de.dentrassi.kura.addons.drools.component.wires.DroolsWirePublisher;

public class DroolsWirePublisherImpl implements DroolsWirePublisher {
    private final WireSupport wire;

    public DroolsWirePublisherImpl(final WireSupport wire) {
        this.wire = wire;
    }

    @Override
    public void publish(final List<WireRecord> records) {
        if (records == null) {
            return;
        }

        this.wire.emit(records);
    }
}
