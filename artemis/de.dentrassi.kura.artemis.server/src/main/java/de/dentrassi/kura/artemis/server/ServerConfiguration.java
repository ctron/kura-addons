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

import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;

public class ServerConfiguration {

    private String securityDomain = "artemis";

    private String brokerXml;

    private Set<String> requiredProtocols = new HashSet<>();

    private SecurityConfiguration securityConfiguration = new SecurityConfiguration();

    public void setSecurityDomain(final String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public void setBrokerXml(final String brokerXml) {
        this.brokerXml = brokerXml;
    }

    public String getBrokerXml() {
        return this.brokerXml;
    }

    public void setRequiredProtocols(final Set<String> requiredProtocols) {
        this.requiredProtocols = requiredProtocols;
    }

    public Set<String> getRequiredProtocols() {
        return this.requiredProtocols;
    }

    public void setSecurityConfiguration(final SecurityConfiguration securityConfiguration) {
        this.securityConfiguration = securityConfiguration;
    }

    public SecurityConfiguration getSecurityConfiguration() {
        return this.securityConfiguration;
    }

}
