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
package org.wildfly.swarm.container;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.wildfly.swarm.container.InputStreamHelper.*;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Bob McWhirter
 */
public class JBossDeploymentStructureAssetTest {


    @Test
    public void testEmpty() throws Exception {
        JBossDeploymentStructureAsset asset = new JBossDeploymentStructureAsset();
        asset.addModule("com.mycorp", "main");

        List<String> lines = read(asset.openStream());

        assertThat( lines ).contains( "<module name=\"com.mycorp\" slot=\"main\"/>");
    }

    @Test
    public void testAdditive() throws Exception {
        JBossDeploymentStructureAsset asset = new JBossDeploymentStructureAsset();
        asset.addModule("com.mycorp", "main");

        JBossDeploymentStructureAsset asset2 = new JBossDeploymentStructureAsset( asset.openStream() );
        asset2.addModule( "com.mycorp.another", "api" );

        List<String> lines = read(asset2.openStream());

        assertThat( lines ).contains( "<module name=\"com.mycorp\" slot=\"main\"/>");
        assertThat( lines ).contains( "<module name=\"com.mycorp.another\" slot=\"api\"/>");

    }


}
