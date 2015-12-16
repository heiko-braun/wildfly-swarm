/*
 * Copyright 2015 Red Hat, Inc, and individual contributors.
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
package org.wildfly.swarm.arquillian.adapter;

import org.jboss.arquillian.container.test.impl.RemoteExtensionLoader;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ExtensionLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class WildFlySwarmDeploymentAppender extends CachedAuxilliaryArchiveAppender {
    @Override
    protected Archive<?> buildArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackages(
                        true,
//                        "org.jboss.arquillian.core",
//                        "org.jboss.arquillian.container.spi",
//                        "org.jboss.arquillian.container.impl",
//                        "org.jboss.arquillian.container.test.api",
                        "org.jboss.arquillian.container.test.spi",
                        "org.wildfly.swarm.arquillian.resources"
//                        "org.jboss.arquillian.container.test.impl",
//                        "org.jboss.arquillian.config",
//                        "org.jboss.arquillian.test",
//                        "org.jboss.shrinkwrap.api",
//                        "org.jboss.shrinkwrap.descriptor.api"
                )
                .addClass(WildFlySwarmRemoteExtension.class)
                .addAsServiceProvider(RemoteLoadableExtension.class, WildFlySwarmRemoteExtension.class)
                .addAsServiceProvider(ExtensionLoader.class, RemoteExtensionLoader.class);
    }
}
