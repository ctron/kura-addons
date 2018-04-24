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

import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import de.dentrassi.kura.addons.drools.Drools;

public class DroolsImpl implements Drools {

    @SuppressWarnings("deprecation")
    private static final class KieBaseBuilderImplementation implements KieBaseBuilder {

        private final ClassLoader classLoader;

        private Properties baseConfigurationProperties;
        private Consumer<KieBaseConfiguration> customizeBaseConfiguration;
        private Consumer<KieFileSystem> fileSystemCustomizer;
        private ReleaseId releaseId;

        public KieBaseBuilderImplementation(final BundleContext context) {
            this.classLoader = context.getBundle().adapt(BundleWiring.class).getClassLoader();
        }

        @Override
        public KieBaseBuilder baseConfigurationProperties(
                final Properties baseConfigurationProperties) {
            this.baseConfigurationProperties = baseConfigurationProperties;
            return this;
        }

        @Override
        public KieBaseBuilder customizeBaseConfiguration(
                final Consumer<KieBaseConfiguration> customizeBaseConfiguration) {
            this.customizeBaseConfiguration = customizeBaseConfiguration;
            return this;
        }

        @Override
        public KieBaseBuilder fileSystem(final Consumer<KieFileSystem> fileSystem) {
            this.fileSystemCustomizer = fileSystem;
            return this;
        }

        @Override
        public KieBaseBuilder releaseId(final ReleaseId releaseId) {
            this.releaseId = releaseId;
            return this;
        }

        @Override
        public KieBase build() {
            return callWithClassLoader(this.classLoader, this::buildBase);
        }

        private KieBase buildBase() {

            final KieServices services = KieServices.get();

            // new file system

            final KieFileSystem fileSystem = services.newKieFileSystem();

            if (this.fileSystemCustomizer != null) {
                this.fileSystemCustomizer.accept(fileSystem);
            }

            // new container

            final ReleaseId release;
            if (this.releaseId == null) {
                release = services.getRepository().getDefaultReleaseId();
            } else {
                release = this.releaseId;
            }

            final KieContainer container = services.newKieContainer(release, this.classLoader);

            // new base configuration

            final KieBaseConfiguration baseConfiguration = services
                    .newKieBaseConfiguration(this.baseConfigurationProperties);

            if (this.customizeBaseConfiguration != null) {
                this.customizeBaseConfiguration.accept(baseConfiguration);
            }

            // new base

            return container.newKieBase(baseConfiguration);
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public KieBaseBuilder newKieBaseBuilder(final BundleContext context) {
        return new KieBaseBuilderImplementation(context);
    }

    private static final class KnowledgeBuilderBaseBuilderImplementation implements KnowledgeBuilderBaseBuilder {

        private Consumer<KnowledgeBuilder> customizer;

        private final ClassLoader classLoader;

        private KnowledgeBuilderConfiguration configuration;
        private KieBaseConfiguration baseConfiguration;

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

        @SuppressWarnings("deprecation")
        @Override
        public KnowledgeBuilderBaseBuilder configureBase(final Properties properties,
                final Consumer<KieBaseConfiguration> customizer) {

            /*
             * Despite the fact that the function is deprecated, it works _and_ is required
             */
            this.baseConfiguration = KieServices.get().newKieBaseConfiguration(properties, this.classLoader);

            if (customizer != null) {
                customizer.accept(this.baseConfiguration);
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
            // should at some point allow to cache this.

            final KnowledgeBuilderImpl builder = (KnowledgeBuilderImpl) KnowledgeBuilderFactory
                    .newKnowledgeBuilder(this.configuration);

            if (this.customizer != null) {
                this.customizer.accept(builder);
            }

            return callWithClassLoader(this.classLoader, () -> builder.newKnowledgeBase(this.baseConfiguration));
        }

    }

    @Override
    public KnowledgeBuilderBaseBuilder newKnowledgeBuilderBaseBuilder(final BundleContext context) {
        return new KnowledgeBuilderBaseBuilderImplementation(context);
    }
}
