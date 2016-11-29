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

package de.dentrassi.kura.artemis.server.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ProtocolTracker {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolTracker.class);

    private final BundleContext context;

    private final Multimap<String, ProtocolManagerFactory<?>> protocols = HashMultimap.create();

    @SuppressWarnings("rawtypes")
    private final ServiceTrackerCustomizer<ProtocolManagerFactory, ProtocolManagerFactory> customizer = new ServiceTrackerCustomizer<ProtocolManagerFactory, ProtocolManagerFactory>() {

        @Override
        public ProtocolManagerFactory addingService(final ServiceReference<ProtocolManagerFactory> reference) {
            final ProtocolManagerFactory service = ProtocolTracker.this.context.getService(reference);
            addProtocols(service);
            return service;
        }

        @Override
        public void modifiedService(final ServiceReference<ProtocolManagerFactory> reference,
                final ProtocolManagerFactory service) {
        }

        @Override
        public void removedService(final ServiceReference<ProtocolManagerFactory> reference,
                final ProtocolManagerFactory service) {
            removeProtocols(service);
            ProtocolTracker.this.context.ungetService(reference);
        }

    };

    @SuppressWarnings("rawtypes")
    private final ServiceTracker<ProtocolManagerFactory, ProtocolManagerFactory> tracker;

    private final ProtocolTrackerListener listener;

    public ProtocolTracker(final BundleContext context, final ProtocolTrackerListener listener) {
        this.context = context;
        this.listener = listener;
        this.tracker = new ServiceTracker<>(context, ProtocolManagerFactory.class, this.customizer);
    }

    public void start() {
        this.tracker.open();
    }

    public void stop() {
        this.tracker.close();
    }

    protected synchronized void addProtocols(final ProtocolManagerFactory<?> factory) {
        final Set<String> protocols = new HashSet<>(Arrays.asList(factory.getProtocols()));

        for (final String protocol : protocols) {
            logger.info("Adding protocol - {} -> {}", protocol, factory);
            this.protocols.put(protocol, factory);
        }

        if (this.listener != null) {
            this.listener.protocolsAdded(protocols);
        }
    }

    protected synchronized void removeProtocols(final ProtocolManagerFactory<?> factory) {
        final Set<String> protocols = new HashSet<>(Arrays.asList(factory.getProtocols()));

        for (final String protocol : protocols) {
            logger.info("Removing protocol - {} -> {}", protocol, factory);
            this.protocols.remove(protocol, factory);
        }

        if (this.listener != null) {
            this.listener.protocolsRemoved(protocols);
        }
    }

    public synchronized Collection<ProtocolManagerFactory<?>> resolveProtocols(final Set<String> requiredProtocols) {
        final Map<String, ProtocolManagerFactory<?>> result = new HashMap<>();

        for (final String required : requiredProtocols) {
            final Collection<ProtocolManagerFactory<?>> factories = this.protocols.get(required);
            if (factories.isEmpty()) {
                // return "unresolved"
                return null;
            }

            // just get the first one
            result.put(required, factories.iterator().next());
        }

        // we are resolved now ... add all others

        for (final Map.Entry<String, ProtocolManagerFactory<?>> entry : this.protocols.entries()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        // return the result

        return result.values();
    }
}
