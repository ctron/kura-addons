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

import static de.dentrassi.kura.addons.drools.component.Filters.simpleFilter;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.scada.utils.osgi.SingleServiceTracker;
import org.kie.api.runtime.KieSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.dentrassi.kura.addons.drools.Configuration;
import de.dentrassi.kura.addons.drools.component.AbstractWireComponent;

public abstract class AbstractDroolsWireComponent extends AbstractWireComponent {

    protected final BundleContext context = FrameworkUtil.getBundle(DroolsListen.class).getBundleContext();

    private SingleServiceTracker<KieSession> tracker;

    protected void setKieSession(final ServiceReference<KieSession> serviceReference,
            final KieSession session) {

    }

    protected void closeSessionTracker() {
        if (this.tracker != null) {
            this.tracker.close();
            this.tracker = null;
        }
    }

    protected void openSessionTracker(final String sessionId) throws InvalidSyntaxException {
        closeSessionTracker();

        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }

        final Filter filter = this.context
                .createFilter(simpleFilter(KieSession.class, "drools.session.id", sessionId));

        this.tracker = new SingleServiceTracker<>(this.context, filter, this::setKieSession);
        this.tracker.open();
    }

    @Override
    protected void activate(final Map<String, ?> properties) throws Exception {

        super.activate(properties);

        openSessionTracker(Configuration.asString(properties, "session.id"));
    }

    @Override
    protected void deactivate() {

        closeSessionTracker();

        super.deactivate();
    }

    protected void withSession(final Consumer<KieSession> consumer) {
        final KieSession session = this.tracker.getService();

        if (session != null) {
            consumer.accept(session);
        }
    }
}
