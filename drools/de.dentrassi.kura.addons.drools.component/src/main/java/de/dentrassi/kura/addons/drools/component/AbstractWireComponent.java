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

package de.dentrassi.kura.addons.drools.component;

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.wireadmin.Wire;

public abstract class AbstractWireComponent implements WireComponent, ConfigurableComponent {

    private WireHelperService wireHelperService;

    protected WireSupport wireSupport;

    public void setWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    protected void activate(final Map<String, ?> properties) throws Exception {
        this.wireSupport = this.wireHelperService.newWireSupport(this);
    }

    protected void modified(final Map<String, ?> properties) throws Exception {
        deactivate();
        activate(properties);
    }

    protected void deactivate() {
        this.wireSupport = null;
    }

    /*
     * For subclasses implementing WireReceiver
     */
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    /*
     * For subclasses implementing WireReceiver
     */
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /*
     * For subclasses implementing WireEmitter
     */
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /*
     * For subclasses implementing WireEmitter
     */
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /*
     * For subclasses implementing WireReceiver
     */
    public void onWireReceive(final WireEnvelope wireEnvelope) {
    }
}