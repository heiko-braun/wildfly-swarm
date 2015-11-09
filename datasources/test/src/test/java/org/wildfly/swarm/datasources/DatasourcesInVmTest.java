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
package org.wildfly.swarm.datasources;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Test;
import org.wildfly.swarm.container.Container;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * @author Bob McWhirter
 */
public class DatasourcesInVmTest {

    @Test
    public void testSimple() throws Exception {
        Container container = new Container();
        container.fraction( new DatasourcesFraction() );
        container.start().stop();
    }

    @Test
    public void testXMLConfig() throws Exception {

        ClassLoader cl = DatasourcesInVmTest.class.getClassLoader();
        Container container = new Container(false, cl.getResource("standalone.xml"));
        container.fraction( new DatasourcesFraction() );
        container.start().stop();
    }

    @Test
    public void testParser() throws Exception {
        // the actual parsing
        ClassLoader cl = DatasourcesInVmTest.class.getClassLoader();
        URL xmlConfig = cl.getResource("standalone.xml");
        InputStream input = xmlConfig.openStream();
        try {
            final List operationList = new ArrayList();
            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            XMLMapper xmlMapper = XMLMapper.Factory.create();

            xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0","server"), new DelegateReader());
            xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0","extensions"), new DelegateReader());
            xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0","management"), new DelegateReader());
            xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0","profile"), new ProfileReader());
            xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0","interfaces"), new DelegateReader());
            xmlMapper.registerRootElement(new QName("urn:jboss:domain:4.0","socket-binding-group"), new DelegateReader());

            xmlMapper.parseDocument(operationList, reader);

            System.out.println(">>" + operationList.size());

        } finally {
            if(input!=null) input.close();
        }

    }

    class DelegateReader implements XMLElementReader {

        Set<String> delegateFurther;

        public DelegateReader() {
            this.delegateFurther = new HashSet<>();
            this.delegateFurther.add("profile");
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, Object o) throws XMLStreamException {

            while (reader.hasNext()) {
                switch (reader.next()) {
                    case COMMENT:
                        break;
                    case END_ELEMENT:
                        return;
                    case START_ELEMENT:
                        String localName = reader.getLocalName();
                        if(delegateFurther.contains(localName)) {
                            reader.handleAny(o);
                        }
                        else {
                            System.out.println("Skip " + reader.getNamespaceURI() + "::" + localName);
                            reader.discardRemainder();
                        }
                        break;
                }
            }
        }
    }

    class ProfileReader implements XMLElementReader {

        @Override
        public void readElement(XMLExtendedStreamReader reader, Object o) throws XMLStreamException {

            int event = reader.getEventType();
            while(true){
                switch(event) {
                    case XMLStreamConstants.START_ELEMENT:

                        if(reader.getLocalName().equals("profile"))  // skip to profile contents
                            break;


                        QName lookup = new QName(reader.getNamespaceURI(), reader.getLocalName());
                        System.out.println("Parsing " + lookup);
                        try {
                            reader.handleAny(o);
                        } catch (XMLStreamException e) {
                            // ignore
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        break;
                }

                if (!reader.hasNext())
                    break;

                event = reader.next();
            }
        }
    }

}
