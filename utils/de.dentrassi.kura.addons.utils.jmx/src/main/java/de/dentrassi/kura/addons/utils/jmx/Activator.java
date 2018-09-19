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

package de.dentrassi.kura.addons.utils.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration<?> handle;

    @Override
    public void start(final BundleContext context) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        if (server != null) {
            this.handle = context.registerService(MBeanServer.class.getName(), server, null);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (this.handle != null) {
            this.handle.unregister();
            this.handle = null;
        }
    }

}
