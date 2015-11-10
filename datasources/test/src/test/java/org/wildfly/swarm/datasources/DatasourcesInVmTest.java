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

import junit.framework.Assert;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.container.runtime.StandaloneXmlParser;

import javax.xml.namespace.QName;
import java.net.URL;
import java.util.List;

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
        container.fraction(new DatasourcesFraction());
        container.start().stop();
    }

    @Test
    public void testParser() throws Exception {
        // the actual parsing
        ClassLoader cl = DatasourcesInVmTest.class.getClassLoader();
        URL xmlConfig = cl.getResource("standalone.xml");
        StandaloneXmlParser parser = new StandaloneXmlParser();
        parser.addDelegate(
                new QName("urn:jboss:domain:datasources:4.0", "subsystem"),
                new DataSourcesExtension.DataSourceSubsystemParser()
        );
        List<ModelNode> operationList = parser.parse(xmlConfig);

        //operationList.forEach(System.out::println);
        Assert.assertEquals("Wrong number of add operations", 3, operationList.size());
    }


}
