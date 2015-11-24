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
package org.wildfly.swarm.ee.runtime;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.config.runtime.invocation.Marshaller;
import org.wildfly.swarm.container.runtime.AbstractParserFactory;
import org.wildfly.swarm.container.runtime.AbstractServerConfiguration;
import org.wildfly.swarm.ee.EEFraction;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Bob McWhirter
 * @author Lance Ball
 */
public class EEConfiguration extends AbstractServerConfiguration<EEFraction> {

    public EEConfiguration() {
        super(EEFraction.class);
    }

    @Override
    public EEFraction defaultFraction() {
        return EEFraction.createDefaultFraction();
    }

    @Override
    public List<ModelNode> getList(EEFraction fraction) throws Exception {

        List<ModelNode> list = new ArrayList<>();

        list.addAll(Marshaller.marshal(fraction));

        return list;

    }

    @Override
    public Optional<ModelNode> getExtension() {
        ModelNode node = new ModelNode();
        node.get(OP_ADDR).add(EXTENSION, "org.jboss.as.ee");
        node.get(OP).set(ADD);
        return Optional.of(node);
    }

    @Override
    public Optional<Map<QName, XMLElementReader<List<ModelNode>>>> getSubsystemParsers() throws Exception {
        return AbstractParserFactory.mapParserNamespaces(new EEParserFactory());
    }
}
