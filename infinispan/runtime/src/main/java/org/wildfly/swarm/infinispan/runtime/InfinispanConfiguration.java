/**
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
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
package org.wildfly.swarm.infinispan.runtime;

import org.jboss.dmr.ModelNode;
import org.wildfly.swarm.container.runtime.MarshallingServerConfiguration;
import org.wildfly.swarm.infinispan.InfinispanFraction;

import java.util.Optional;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Lance Ball
 */
public class InfinispanConfiguration extends MarshallingServerConfiguration<InfinispanFraction> {

    public static final String EXTENSION_MODULE = "org.jboss.as.clustering.infinispan";

    public InfinispanConfiguration() {
        super(InfinispanFraction.class, EXTENSION_MODULE);
    }

    @Override
    public InfinispanFraction defaultFraction() {
        return InfinispanFraction.createDefaultFraction();
    }

    @Override
    public Optional<ModelNode> getExtension() {
        ModelNode node = new ModelNode();
        node.get(OP_ADDR).add(EXTENSION, "org.jboss.as.clustering.infinispan");
        node.get(OP).set(ADD);
        return Optional.of(node);
    }
}
