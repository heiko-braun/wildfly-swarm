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
package org.wildfly.swarm.transactions.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.config.runtime.invocation.Marshaller;
import org.wildfly.swarm.container.runtime.AbstractServerConfiguration;
import org.wildfly.swarm.transactions.TransactionsFraction;

import javax.xml.namespace.QName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;

/**
 * @author Bob McWhirter
 */
public class TransactionsConfiguration extends AbstractServerConfiguration<TransactionsFraction> {

    public TransactionsConfiguration() {
        super(TransactionsFraction.class);
    }

    @Override
    public TransactionsFraction defaultFraction() {

        return TransactionsFraction.createDefaultFraction();
    }

    @Override
    public List<ModelNode> getList(TransactionsFraction fraction) throws Exception {
        List<ModelNode> list = new ArrayList<>();

        list.addAll(Marshaller.marshal(fraction));

        ModelNode node = new ModelNode();
        node.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement("socket-binding-group", "default-sockets")).append("socket-binding", "txn-recovery-environment").toModelNode());
        node.get(OP).set(ADD);
        node.get(PORT).set(fraction.getPort());
        list.add(node);

        node = new ModelNode();
        node.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement("socket-binding-group", "default-sockets")).append("socket-binding", "txn-status-manager").toModelNode());
        node.get(OP).set(ADD);
        node.get(PORT).set(fraction.getStatusPort());
        list.add(node);

        return list;
    }

    @Override
    public Optional<ModelNode> getExtension() {
        ModelNode node = new ModelNode();
        node.get(OP_ADDR).add(EXTENSION, "org.jboss.as.transactions");
        node.get(OP).set(ADD);
        return Optional.of(node);
    }

    @Override
    public Optional<Map<QName, XMLElementReader<List<ModelNode>>>> getSubsystemParsers() throws Exception {
        Map<QName, XMLElementReader<List<ModelNode>>> map = new HashMap<>();
        map.put(
                new QName("urn:jboss:domain:transactions:3.0", "subsystem"),
                new TransactionParserFactory().create()
        );
        return Optional.of(map);
    }
}
