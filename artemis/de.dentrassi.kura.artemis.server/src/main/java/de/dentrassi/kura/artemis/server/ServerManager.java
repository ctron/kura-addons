/*
 * Copyright (C) 2016 Jens Reimann <jreimann@redhat.com>
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

package de.dentrassi.kura.artemis.server;

import java.util.Collection;
import java.util.Set;

import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dentrassi.kura.artemis.server.internal.ProtocolTracker;
import de.dentrassi.kura.artemis.server.internal.ProtocolTrackerListener;

public class ServerManager {

    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);

    private final ServerConfiguration configuration;
    private final ProtocolTracker protocolTracker;

    private final ProtocolTrackerListener listener = new ProtocolTrackerListener() {

        @Override
        public void protocolsAdded(final Set<String> protocols) {
            ServerManager.this.protocolsAdded(protocols);
        }

        @Override
        public void protocolsRemoved(final Set<String> protocols) {
            ServerManager.this.protocolsRemoved(protocols);
        }

    };

    private ServerRunner runner;

    public ServerManager(final ServerConfiguration configuration) {
        this.configuration = configuration;

        this.protocolTracker = new ProtocolTracker(FrameworkUtil.getBundle(ServerManager.class).getBundleContext(),
                this.listener);
    }

    public synchronized void start() throws Exception {
        this.protocolTracker.start();
        testStart();
    }

    public synchronized void stop() throws Exception {
        performStop();
        this.protocolTracker.stop();
    }

    protected synchronized void protocolsAdded(final Set<String> protocols) {
        logger.info("Protocols added - {}", protocols);
        try {
            testStart();
        } catch (final Exception e) {
            logger.warn("Failed to start", e);
        }
    }

    protected synchronized void protocolsRemoved(final Set<String> protocols) {
        logger.info("Protocols removed - {}", protocols);
        try {
            testStop();
        } catch (final Exception e) {
            logger.warn("Failed to stop", e);
        }
    }

    private void testStart() throws Exception {
        if (this.runner != null) {
            logger.debug("Already running");
            return;
        }

        final Collection<ProtocolManagerFactory<?>> protocols = this.protocolTracker
                .resolveProtocols(this.configuration.getRequiredProtocols());

        if (protocols == null) {
            // FIXME: better error message
            logger.warn("Unable to resolve protocols: {}", this.configuration.getRequiredProtocols());
            return;
        }

        this.runner = new ServerRunner(this.configuration, protocols);
        this.runner.start();

    }

    private void testStop() throws Exception {
        if (this.runner == null) {
            logger.debug("Not running anyway");
            return;
        }

        final Collection<ProtocolManagerFactory<?>> protocols = this.protocolTracker
                .resolveProtocols(this.configuration.getRequiredProtocols());

        if (protocols != null) {
            return;
        }

        performStop();
    }

    private void performStop() throws Exception {
        this.runner.stop();
        this.runner = null;
    }

}
