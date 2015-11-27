package org.wildfly.swarm.container.runtime;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.RuntimeHostControllerInfoAccessor;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.server.parsing.ExtensionHandler;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 27/11/15
 */
public class StandaloneXmlParser2 {


    /**
     * Used to ignore certain elements
     */
    public final static XMLElementReader<List<ModelNode>> NOOP_READER = new NoopXMLElementReader();

    private final XMLMapper xmlMapper;
    private final StandaloneXml parserDelegate;
    private final ExtensionRegistry registry;

    public StandaloneXmlParser2() {

        RunningModeControl runningModeControl = new RunningModeControl(RunningMode.NORMAL);
        registry = new ExtensionRegistry(
                ProcessType.STANDALONE_SERVER,
                runningModeControl, null, null,
                RuntimeHostControllerInfoAccessor.SERVER
        );

        parserDelegate = new StandaloneXml(new ExtensionHandler() {
            @Override
            public void parseExtensions(XMLExtendedStreamReader reader, ModelNode address, Namespace namespace, List<ModelNode> list) throws XMLStreamException {
                reader.discardRemainder(); // noop
            }

            @Override
            public ExtensionRegistry getRegistry() {
                return registry;
            }

            @Override
            public void writeExtensions(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
                // noop
            }
        });

        xmlMapper = XMLMapper.Factory.create();
        xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0", "server"), parserDelegate);

    }

    /**
     * Add a parser for a subpart of the XML model.
     *
     * @param elementName the FQ element name (i..e subsystem name)
     * @param parser creates ModelNode's from XML input
     * @return
     */
    public StandaloneXmlParser2 addDelegate(QName elementName, XMLElementReader<List<ModelNode>> parser) {
        xmlMapper.registerRootElement(elementName, parser);
        return this;
    }

    public List<ModelNode> parse(URL xml) throws Exception {

        final List<ModelNode> operationList = new ArrayList<>();

        InputStream input = null;
        try {
            input = xml.openStream();

            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);

            xmlMapper.parseDocument(operationList, reader);

            //operationList.forEach(System.out::println);

        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                if (input != null) input.close();
            } catch (IOException e) {
                // ignore
            }
        }

        return operationList;
    }

    private static class NoopXMLElementReader implements XMLElementReader<List<ModelNode>> {
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> modelNode) throws XMLStreamException {
            System.out.println("Skip "+reader.getNamespaceURI()+"::"+reader.getLocalName());
            reader.discardRemainder();
        }
    }

}
