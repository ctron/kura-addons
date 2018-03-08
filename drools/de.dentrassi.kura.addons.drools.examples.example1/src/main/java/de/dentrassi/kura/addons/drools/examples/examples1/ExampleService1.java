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

package de.dentrassi.kura.addons.drools.examples.examples1;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;
import java.util.Random;

import org.drools.core.io.impl.ClassPathResource;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.conf.ClassLoaderCacheOption;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import de.dentrassi.kura.addons.drools.Drools;

@Designate(ocd = Config.class)
@Component(immediate = true, configurationPolicy = REQUIRE, property = { "osgi.command.scope=drools",
        "osgi.command.function=test1" })
public class ExampleService1 implements ConfigurableComponent {

    private KieSession session;

    private BarFoo result = new BarFoo();

    private Drools drools;

    private KieBase base;

    @Reference
    public void setDrools(final Drools drools) {
        this.drools = drools;
    }

    @Activate
    public void activate(final BundleContext context, final Map<String, Object> properties) {

        this.base = drools.newKnowledgeBuilderBaseBuilder(context)

                .configure(cfg -> {
                    cfg.setOption(ClassLoaderCacheOption.DISABLED);
                })

                .customize(builder -> {
                    builder.add(new ClassPathResource("example1.drl", ExampleService1.class), ResourceType.DRL);
                })

                .build();

        modified(properties);
    }

    @Deactivate
    public void deactivate() {
        if (session != null) {
            try {
                session.dispose();
            } finally {
                session = null;
            }
        }
    }

    @Modified
    public void modified(final Map<String, Object> properties) {

        boolean enabled = Boolean.parseBoolean("" + properties.get("enabled"));

        if (enabled && session == null) {
            this.session = base.newKieSession();
            this.session.setGlobal("barFoo", this.result);
        } else if (!enabled && session != null) {
            session.dispose();
            session = null;
        }

    }

    public void test1() {
        final KieSession session = this.session;

        if (session == null) {
            System.out.println("No active session");
            return;
        }

        final FooBar input = new FooBar();
        input.setValue(new Random().nextInt());
        session.insert(input);
        session.fireAllRules();

        System.out.println("Result: " + result.getResult());
    }

}
