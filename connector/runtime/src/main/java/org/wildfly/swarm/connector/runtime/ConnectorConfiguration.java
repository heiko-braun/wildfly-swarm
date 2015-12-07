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
package org.wildfly.swarm.connector.runtime;

import java.util.Optional;

import org.jboss.dmr.ModelNode;
import org.wildfly.swarm.connector.ConnectorFraction;
import org.wildfly.swarm.container.runtime.AbstractServerConfiguration;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Bob McWhirter
 */
public class ConnectorConfiguration extends AbstractServerConfiguration<ConnectorFraction> {

    private static final String EXTENSION_NAME = "org.jboss.as.connector";

    public ConnectorConfiguration() {
        super(ConnectorFraction.class);
    }

    @Override
    public ConnectorFraction defaultFraction() {
        return new ConnectorFraction();
    }

    @Override
    public Optional<ModelNode> getExtension() {
        ModelNode node = new ModelNode();
        node.get(OP_ADDR).set(EXTENSION, EXTENSION_NAME);
        node.get(OP).set(ADD);

        return Optional.of(node);
    }
}
