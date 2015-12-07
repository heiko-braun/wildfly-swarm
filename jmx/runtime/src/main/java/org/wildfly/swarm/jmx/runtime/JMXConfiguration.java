/**
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
package org.wildfly.swarm.jmx.runtime;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.swarm.container.runtime.AbstractServerConfiguration;
import org.wildfly.swarm.jmx.JMXFraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Bob McWhirter
 */
public class JMXConfiguration extends AbstractServerConfiguration<JMXFraction> {

    public JMXConfiguration() {
        super(JMXFraction.class);
    }

    @Override
    public JMXFraction defaultFraction() {
        return new JMXFraction();
    }

    @Override
    public List<ModelNode> getList(JMXFraction fraction) {
        List<ModelNode> list = new ArrayList<>();

        PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "jmx"));

        ModelNode node = new ModelNode();
        node.get(OP_ADDR).set(address.toModelNode());
        node.get(OP).set(ADD);
        list.add(node);

        return list;

    }

    @Override
    public Optional<ModelNode> getExtension() {
        ModelNode node = new ModelNode();
        node.get(OP_ADDR).add(EXTENSION, "org.jboss.as.jmx");
        node.get(OP).set(ADD);

        return Optional.of(node);
    }
}
