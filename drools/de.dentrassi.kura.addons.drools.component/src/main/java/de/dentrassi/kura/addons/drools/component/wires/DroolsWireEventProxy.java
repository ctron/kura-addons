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

package de.dentrassi.kura.addons.drools.component.wires;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.wire.WireRecord;

public interface DroolsWireEventProxy {

    public void publish(List<WireRecord> records);

    public default void publish(final WireRecord record) {
        publish(Collections.singletonList(record));
    }

    public default void publish(final WireRecord[] records) {
        if (records == null) {
            return;
        }

        publish(Arrays.asList(records));
    }

    public default void publish(final Collection<WireRecord> records) {

        if (records == null) {
            return;
        }

        if (records instanceof List<?>) {
            publish((List<WireRecord>) records);
        } else {
            publish(new ArrayList<>(records));
        }
    }
}
