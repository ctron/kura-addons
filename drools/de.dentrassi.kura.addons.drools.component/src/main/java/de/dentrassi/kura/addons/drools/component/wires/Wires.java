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

import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;

public final class Wires {

    private Wires() {
    }

    public static Map<String, TypedValue<?>> toMap(final WireEnvelope envelope) {
        return toMap(envelope.getRecords());
    }

    public static Map<String, TypedValue<?>> toMap(final Collection<WireRecord> records) {
        return records.stream()
                .flatMap(record -> record.getProperties().entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    }

    public static List<WireRecord> toRecords(final Map<String, TypedValue<?>> result) {
        return singletonList(new WireRecord(result));
    }
}
