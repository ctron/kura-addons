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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Meta type information for {@link CamelExampleConfigurableComponent}
 * <p>
 * <strong>Note: </strong> The id must be the full qualified name of the
 * assigned component.
 * </p>
 */
@ObjectClassDefinition(id = "de.dentrassi.kura.addons.utils.jolokia.JolokiaComponent", name = "Jolokia API", description = "The Jolokia API")
@interface Config {

    @AttributeDefinition(name = "Enable", required = true, description = "Enables the Jolokia API.")
    public boolean enabled() default true;

    @AttributeDefinition(name = "Username", required = true, description = "The username used for authenticating clients.")
    public String user() default "jolokia";

    @AttributeDefinition(name = "Password", required = true, description = "The password used for authenticating clients.", type = AttributeType.PASSWORD)
    public String password();

}