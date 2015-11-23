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
package org.wildfly.swarm.logging.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.config.runtime.invocation.Marshaller;
import org.wildfly.swarm.container.runtime.AbstractServerConfiguration;
import org.wildfly.swarm.logging.LoggingFraction;

import javax.xml.namespace.QName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Bob McWhirter
 * @author Lance Ball
 */
public class LoggingConfiguration extends AbstractServerConfiguration<LoggingFraction> {

    public LoggingConfiguration() {
        super(LoggingFraction.class);
    }

    @Override
    public LoggingFraction defaultFraction() {
        String prop = System.getProperty("swarm.logging");
        if (prop != null) {
            prop = prop.trim().toLowerCase();

            if (prop.equals("debug")) {
                return LoggingFraction.createDebugLoggingFraction();
            } else if (prop.equals("trace")) {
                return LoggingFraction.createTraceLoggingFraction();
            }
        }

        return LoggingFraction.createDefaultLoggingFraction();
    }

    @Override
    public List<ModelNode> getList(LoggingFraction fraction) throws Exception {
        if (fraction == null) {
            fraction = defaultFraction();
        }

        List<ModelNode> list = new ArrayList<>();
        list.addAll(Marshaller.marshal(fraction));
        return list;
    }

    @Override
    public Optional<ModelNode> getExtension() {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).add(EXTENSION, "org.jboss.as.logging");
        op.get(OP).set(ADD);

        return Optional.of(op);
    }

    @Override
    public Optional<Map<QName, XMLElementReader<List<ModelNode>>>> getSubsystemParsers() throws Exception {
        Map<QName, XMLElementReader<List<ModelNode>>> map = new HashMap<>();

        new LoggingParserFactory().create().forEach((qName, XMLElementReader) -> {
            map.put(new QName(qName.getNamespaceURI(), "subsystem"), XMLElementReader);
        });

        return Optional.of(map);
    }
}
