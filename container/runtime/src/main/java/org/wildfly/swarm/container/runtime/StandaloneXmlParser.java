package org.wildfly.swarm.container.runtime;

import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.parsing.ParseUtils.*;

/**
 * A derivation of the parser in wildfly core without the bells and whistles to load modules on the fly, etc.
 * It currently skips large parts of the model that we don't need in swarm.
 *
 * @see org.jboss.as.server.parsing.StandaloneXml
 *
 * @author Heiko Braun
 * @since 10/11/15
 */
public class StandaloneXmlParser {


    private final XMLMapper xmlMapper;

    public StandaloneXmlParser() {
        xmlMapper = XMLMapper.Factory.create();
        xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0", "server"), new StandaloneXmlReader());
        //xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0", "socket-binding-group"), new SocketBindingParser());
    }

    public List<ModelNode> parse(URL xmlConfig) {
        final List operationList = new ArrayList();
        InputStream input = null;
        try {
            input = xmlConfig.openStream();

            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);

            xmlMapper.parseDocument(operationList, reader);

            //operationList.forEach(System.out::println);

        } catch(Throwable t) {
            System.out.println(t);
        } finally {
            try {
                if(input!=null) input.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return operationList;
    }

    /**
     * Add a parser for a subpart of the XML model.
     *
     * @param elementName the FQ element name (i..e subsystem name)
     * @param parser creates ModelNode's from XML input
     * @return
     */
    public StandaloneXmlParser addDelegate(QName elementName, XMLElementReader<List<ModelNode>> parser) {
        xmlMapper.registerRootElement(elementName, parser);
        return this;
    }

    private final class StandaloneXmlReader implements XMLElementReader<List<ModelNode>> {
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operationList) throws XMLStreamException {

            Namespace namespace = Namespace.forUri(reader.getNamespaceURI());

            Element element = nextElement(reader, namespace);
            if (element == Element.EXTENSIONS) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }

            if (element == Element.SYSTEM_PROPERTIES) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }
            if (element == Element.PATHS) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }

            if (element == Element.VAULT) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }
            if (element == Element.MANAGEMENT) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }

            if (element == Element.PROFILE) {
                parseServerProfile(reader, operationList);
                element = nextElement(reader, namespace);
            }

            if (element == Element.INTERFACES) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }

            if (element == Element.SOCKET_BINDING_GROUP) {
                reader.handleAny(operationList);
                element = nextElement(reader, namespace);
            }

            if (element == Element.DEPLOYMENTS) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }

            if (element == Element.DEPLOYMENT_OVERLAYS) {
                skip(element, reader);
                element = nextElement(reader, namespace);
            }
            if (element != null) {
                throw unexpectedElement(reader);
            }

        }

        private void skip(Element element, XMLExtendedStreamReader reader) throws XMLStreamException {
            System.out.println("Skip "+element);
            reader.discardRemainder();

        }

        private void parseServerProfile(final XMLExtendedStreamReader reader, final List<ModelNode> list)
                throws XMLStreamException {

            // Attributes
            requireNoAttributes(reader);

            // Content
            final Map<String, List<ModelNode>> profileOps = new LinkedHashMap<String, List<ModelNode>>();
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                    throw unexpectedElement(reader);
                }
                String namespace = reader.getNamespaceURI();
                if (profileOps.containsKey(namespace)) {
                    throw ControllerLogger.ROOT_LOGGER.duplicateDeclaration("subsystem", reader.getLocation());
                }
                // parse subsystem
                final List<ModelNode> subsystems = new ArrayList<ModelNode>();
                try {
                    reader.handleAny(subsystems);   // delegates to a XMLElementReader registered in #addDelegate()
                } catch (XMLStreamException e) {
                    QName element = new QName(reader.getNamespaceURI(), reader.getLocalName());
                    System.out.println("Failed to parse "+ element +", skipping ...");
                    reader.discardRemainder();
                }

                profileOps.put(namespace, subsystems);
            }

            for (List<ModelNode> subsystems : profileOps.values()) {
                for (final ModelNode update : subsystems) {
                    // Process relative subsystem path address
                    final ModelNode subsystemAddress = new ModelNode();
                    for (final Property path : update.get(OP_ADDR).asPropertyList()) {
                        subsystemAddress.add(path.getName(), path.getValue().asString());
                    }
                    update.get(OP_ADDR).set(subsystemAddress);
                    list.add(update);
                }
            }
        }
    }

    class SocketBindingParser implements XMLElementReader<List<ModelNode>> {
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> payload) throws XMLStreamException {

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

                final Element element = Element.forName(reader.getLocalName());

                switch (element) {
                    case SOCKET_BINDING: {
                        System.out.println(">> " + reader.getLocalName() + " :: " + reader.getAttributeValue(0));

                        reader.discardRemainder();
                        break;
                    }
                    case OUTBOUND_SOCKET_BINDING: {
                        reader.discardRemainder();
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }
    }
}
