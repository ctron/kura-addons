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

package de.dentrassi.kura.addons.drools.internal;

import static io.glutamate.lang.ClassLoaders.callWithClassLoader;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

import org.kie.api.KieBase;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import de.dentrassi.kura.addons.drools.Drools;

public class DroolsImpl implements Drools {

    private final class KnowledgeBuilderBaseBuilderImplementation implements KnowledgeBuilderBaseBuilder {

        private Consumer<KnowledgeBuilder> customizer;

        private final ClassLoader classLoader;
        private KnowledgeBuilderConfiguration configuration;

        public KnowledgeBuilderBaseBuilderImplementation(final BundleContext context) {
            Objects.requireNonNull(context);

            // get the class loader of the bundle which is our context
            this.classLoader = context.getBundle().adapt(BundleWiring.class).getClassLoader();

            // trigger default configuration
            configure(null, null);
        }

        @Override
        public KnowledgeBuilderBaseBuilder configure(final Properties properties,
                final Consumer<KnowledgeBuilderConfiguration> customizer) {

            this.configuration = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(properties, this.classLoader);
            if (customizer != null) {
                customizer.accept(this.configuration);
            }

            return this;
        }

        @Override
        public KnowledgeBuilderBaseBuilder customize(final Consumer<KnowledgeBuilder> customizer) {
            this.customizer = customizer;
            return this;
        }

        @Override
        public KieBase build() {

            // TODO: this isn't yet optimal as we always create a new builder instance. We
            // should
            // at some point allow to cache this.

            final KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder(this.configuration);

            if (this.customizer != null) {
                this.customizer.accept(builder);
            }

            return callWithClassLoader(this.classLoader, builder::newKieBase);
        }

    }

    @Override
    public KnowledgeBuilderBaseBuilder newKnowledgeBuilderBaseBuilder(final BundleContext context) {
        return new KnowledgeBuilderBaseBuilderImplementation(context);
    }
}
