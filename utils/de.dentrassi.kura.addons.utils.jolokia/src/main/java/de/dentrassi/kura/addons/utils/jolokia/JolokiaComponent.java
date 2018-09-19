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

package de.dentrassi.kura.addons.utils.jolokia;

import java.util.Hashtable;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.jolokia.config.ConfigKey;
import org.jolokia.osgi.security.BasicAuthenticationHttpContext;
import org.jolokia.osgi.security.BasicAuthenticator;
import org.jolokia.osgi.servlet.JolokiaServlet;
import org.jolokia.util.NetworkUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class)
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JolokiaComponent implements ConfigurableComponent {

    private static final String REALM = "jolokia";
    private static final String ALIAS = "/jolokia";

    @Reference
    private HttpService httpService;

    @Reference
    private CryptoService cryptoService;

    private JolokiaServlet servlet;

    private boolean isEnabled(final Config config) {

        if (!config.enabled()) {
            return false;
        }

        if (config.user() == null || config.user().isEmpty()) {
            return false;
        }

        final String password = decodePassword(config);
        if (password == null || password.isEmpty()) {
            return true;
        }

        return true;
    }

    private void startServlet(final Config config, final BundleContext context) throws Exception {

        if (!isEnabled(config)) {
            return;
        }

        final Hashtable<String, Object> initparams = new Hashtable<>();
        initparams.put(ConfigKey.AGENT_ID.getKeyValue(), NetworkUtil.getAgentId(hashCode(), "kura"));

        this.servlet = new JolokiaServlet(context);
        this.httpService.registerServlet(ALIAS, this.servlet, initparams, createHttpContext(config));

    }

    private String decodePassword(final Config config) {
        final String password = config.password();
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            return String.valueOf(this.cryptoService.decryptAes(config.password().toCharArray()));
        } catch (final KuraException e) {
            throw new RuntimeException("Failed to decode password", e);
        }
    }

    private HttpContext createHttpContext(final Config config) throws Exception {
        final BasicAuthenticator authenticator = new BasicAuthenticator(config.user(), decodePassword(config));
        return new BasicAuthenticationHttpContext(REALM, authenticator);
    }

    private void stopServlet() {

        if (this.servlet != null) {
            this.httpService.unregister(ALIAS);
            this.servlet = null;
        }

    }

    @Activate
    protected void activate(final Config config, final BundleContext context) throws Exception {
        startServlet(config, context);
    }

    @Modified
    protected void modified(final Config config, final BundleContext context) throws Exception {
        stopServlet();
        startServlet(config, context);
    }

    @Deactivate
    protected void deactivate() {
    }

}
