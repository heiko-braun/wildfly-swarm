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
package org.wildfly.swarm.netflix.ribbon.runtime;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;

import java.util.List;

/**
 * @author Bob McWhirter
 */
public class ClusterServerList extends AbstractServerList<Server> {

    private String appName;

    @Override
    public void initWithNiwsConfig(IClientConfig config) {
        this.appName = config.getClientName();
    }

    @Override
    public List<Server> getInitialListOfServers() {
        return ClusterRegistry.INSTANCE.getServers( this.appName );
    }

    @Override
    public List<Server> getUpdatedListOfServers() {
        return ClusterRegistry.INSTANCE.getServers( this.appName );
    }
}
