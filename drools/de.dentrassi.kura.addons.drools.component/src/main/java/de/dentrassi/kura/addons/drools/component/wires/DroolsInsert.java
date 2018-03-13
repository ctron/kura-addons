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

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.api.runtime.rule.FactHandle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import de.dentrassi.kura.addons.drools.Configuration;

@Component(enabled = true, configurationPolicy = REQUIRE, service = { ConfigurableComponent.class, WireComponent.class,
        WireReceiver.class, org.osgi.service.wireadmin.Consumer.class }, property = {
                "service.pid=de.dentrassi.kura.addons.drools.component.wires.DroolsInsert" })
public class DroolsInsert extends AbstractDroolsWireComponent implements WireReceiver {

    private String entryPoint;
    private boolean fireAllRules;
    private boolean delete;

    @Override
    @Reference
    public void setWireHelperService(final WireHelperService wireHelperService) {
        super.setWireHelperService(wireHelperService);
    }

    @Override
    @Activate
    protected void activate(final Map<String, ?> properties) throws Exception {
        this.entryPoint = Configuration.asString(properties, "entryPoint");
        this.fireAllRules = Configuration.asBoolean(properties, "fireAllRules", true);
        this.delete = Configuration.asBoolean(properties, "delete");
        super.activate(properties);
    }

    @Override
    @Modified
    protected void modified(final Map<String, ?> properties) throws Exception {
        super.modified(properties);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void onWireReceive(final WireEnvelope envelope) {
        withSession(session -> {

            // process atomic
            session.submit(kieSession -> processReceive(envelope, kieSession));

        });
    }

    private void processReceive(final WireEnvelope envelope, final KieSession session) {
        EntryPoint ep = session;
        if (this.entryPoint != null && !this.entryPoint.isEmpty()) {
            ep = session.getEntryPoint(this.entryPoint);
        }

        if (ep == null) {
            return;
        }

        final FactHandle handle = ep.insert(envelope);

        if (this.fireAllRules) {
            session.fireAllRules();
        }

        if (this.delete) {
            ep.delete(handle);
        }
    }

}
