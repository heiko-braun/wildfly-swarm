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
package org.wildfly.swarm.container.runtime;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.server.SelfContainedContainer;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.vfs.TempFileProvider;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.container.Deployer;
import org.wildfly.swarm.container.Fraction;
import org.wildfly.swarm.container.Interface;
import org.wildfly.swarm.container.RuntimeModuleProvider;
import org.wildfly.swarm.container.Server;
import org.wildfly.swarm.container.SocketBinding;
import org.wildfly.swarm.container.SocketBindingGroup;

import javax.xml.namespace.QName;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.LogManager;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Bob McWhirter
 * @author Ken Finnigan
 */
public class RuntimeServer implements Server {


    private SelfContainedContainer container = new SelfContainedContainer();

    private SimpleContentProvider contentProvider = new SimpleContentProvider();

    private ServiceContainer serviceContainer;

    private ModelControllerClient client;

    private RuntimeDeployer deployer;

    private Map<Class<? extends Fraction>, ServerConfiguration> configByFractionType = new ConcurrentHashMap();

    private List<ServerConfiguration> configList = new ArrayList<>();

    // optional XML config
    private Optional<URL> xmlConfig = Optional.empty();

    public RuntimeServer() {

        try {
            Module loggingModule = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.wildfly.swarm.logging", "runtime"));

            ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loggingModule.getClassLoader());
                System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
                //force logging init
                LogManager.getLogManager();
            } finally {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        } catch (ModuleLoadException e) {
            System.err.println( "[WARN] wildfly-swarm-logging not available, logging will not be configured" );
        }
    }

    @Override
    public void setXmlConfig(URL xmlConfig) {
        if(null==xmlConfig)
            throw new IllegalArgumentException("Invalid XML config");
        this.xmlConfig = Optional.of(xmlConfig);
    }

    @Override
    public Deployer start(Container config) throws Exception {

        UUID uuid = UUIDFactory.getUUID();
        System.setProperty("jboss.server.management.uuid", uuid.toString());

        loadFractionConfigurations();

        applyDefaults(config);

        for (Fraction fraction : config.fractions() ) {
            fraction.postInitialize( config.createPostInitContext() );
        }

        LinkedList<ModelNode> bootstrapOperations = new LinkedList<>();

        // the extensions
        getExtensions(config, bootstrapOperations);

        // the subsystem configurations
        getList(config, bootstrapOperations);

        //System.err.println( list );

        Thread.currentThread().setContextClassLoader(RuntimeServer.class.getClassLoader());

        UUID grist = java.util.UUID.randomUUID();
        String tmpDir = System.getProperty("java.io.tmpdir");
        System.err.println( "tmpDir: " + tmpDir );
        Path gristedTmp = Paths.get(tmpDir).resolve("wildfly-swarm-" + grist);
        System.setProperty( "jboss.server.temp.dir", gristedTmp.toString() );

        ScheduledExecutorService tempFileExecutor = Executors.newSingleThreadScheduledExecutor();
        TempFileProvider tempFileProvider = TempFileProvider.create("wildfly-swarm", tempFileExecutor);
        List<ServiceActivator> activators = new ArrayList<>();
        activators.add(new ServiceActivator() {
            @Override
            public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
                context.getServiceTarget().addService(ServiceName.of("wildfly", "swarm", "temp-provider"), new ValueService<>(new ImmediateValue<Object>(tempFileProvider)))
                        .install();
                // Provide the main command line args as a value service
                context.getServiceTarget().addService(ServiceName.of("wildfly", "swarm", "main-args"), new ValueService<>(new ImmediateValue<Object>(config.getArgs())))
                        .install();
            }
        });

        OUTER:
        for (ServerConfiguration eachConfig : this.configList) {
            boolean found = false;
            INNER:
            for (Fraction eachFraction : config.fractions()) {
                if (eachConfig.getType().isAssignableFrom(eachFraction.getClass())) {
                    found = true;
                    activators.addAll(eachConfig.getServiceActivators(eachFraction));
                    break INNER;
                }
            }
            if (!found && !eachConfig.isIgnorable()) {
                System.err.println("*** unable to find fraction for: " + eachConfig.getType());
            }
        }


        this.serviceContainer = this.container.start(bootstrapOperations, this.contentProvider, activators);
        for (ServiceName serviceName : this.serviceContainer.getServiceNames()) {
            ServiceController<?> serviceController = this.serviceContainer.getService(serviceName);
            if (serviceController.getStartException() != null) {
                throw serviceController.getStartException();
            }
        }
        ModelController controller = (ModelController) this.serviceContainer.getService(Services.JBOSS_SERVER_CONTROLLER).getValue();
        Executor executor = Executors.newSingleThreadExecutor();

        this.client = controller.createClient(executor);
        this.deployer = new RuntimeDeployer(this.configList, this.client, this.contentProvider, tempFileProvider);

        List<Archive> implicitDeployments = new ArrayList<>();

        OUTER:
        for (ServerConfiguration eachConfig : this.configList) {
            INNER:
            for (Fraction eachFraction : config.fractions()) {
                if (eachConfig.getType().isAssignableFrom(eachFraction.getClass())) {
                    implicitDeployments.addAll(eachConfig.getImplicitDeployments( eachFraction ) );
                    break INNER;
                }
            }
        }

        for (Archive each : implicitDeployments) {
            this.deployer.deploy( each );
        }

        return this.deployer;
    }

    public void stop() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        this.serviceContainer.addTerminateListener(new ServiceContainer.TerminateListener() {
            @Override
            public void handleTermination(Info info) {
                latch.countDown();
            }
        });
        this.serviceContainer.shutdown();

        latch.await();

        this.deployer.stop();
        this.serviceContainer = null;
        this.client = null;
        this.deployer = null;
    }

    @Override
    public Set<Class<? extends Fraction>> getFractionTypes() {
        return this.configByFractionType.keySet();
    }

    @Override
    public Fraction createDefaultFor(Class<? extends Fraction> fractionClazz) {
        return this.configByFractionType.get(fractionClazz).defaultFraction();
    }

    private void applyDefaults(Container config) throws Exception {
        config.applyFractionDefaults(this);
        applyInterfaceDefaults(config);
        applySocketBindingGroupDefaults(config);
    }

    private void applyInterfaceDefaults(Container config) {
        if (config.ifaces().isEmpty()) {
            config.iface("public", "${jboss.bind.address:0.0.0.0}");
        }
    }

    private void applySocketBindingGroupDefaults(Container config) {
        if (config.socketBindingGroups().isEmpty()) {
            config.socketBindingGroup(
                    new SocketBindingGroup("default-sockets", "public", "${jboss.socket.binding.port-offset:0}")
            );
        }

        Set<String> groupNames = config.socketBindings().keySet();

        for (String each : groupNames) {
            List<SocketBinding> bindings = config.socketBindings().get(each);
            SocketBindingGroup group = config.getSocketBindingGroup(each);
            if (group == null) {
                throw new RuntimeException("No socket-binding-group for '" + each + "'");
            }

            for (SocketBinding binding : bindings) {
                group.socketBinding(binding);
            }
        }
    }

    private void loadFractionConfigurations() throws Exception {
        Module m1 = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("swarm.application"));
        ServiceLoader<RuntimeModuleProvider> providerLoader = m1.loadService(RuntimeModuleProvider.class);

        Iterator<RuntimeModuleProvider> providerIter = providerLoader.iterator();

        if (!providerIter.hasNext()) {
            providerLoader = ServiceLoader.load(RuntimeModuleProvider.class, ClassLoader.getSystemClassLoader());
            providerIter = providerLoader.iterator();
        }

        while (providerIter.hasNext()) {
            RuntimeModuleProvider provider = providerIter.next();
            Module module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create(provider.getModuleName(), provider.getSlotName()));
            ServiceLoader<ServerConfiguration> configLoaders = module.loadService(ServerConfiguration.class);

            for (ServerConfiguration serverConfig : configLoaders) {
                this.configByFractionType.put(serverConfig.getType(), serverConfig);
                this.configList.add(serverConfig);
            }
        }
    }

    private void getExtensions(Container container, List<ModelNode> list) throws Exception {

        FractionProcessor<List<ModelNode>> consumer = (context, cfg, fraction) -> {
            try {
                Optional<ModelNode> extension = cfg.getExtension();
                extension.map(modelNode -> list.add(modelNode));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        visitFractions(container, list, consumer);

    }

    private void getList(Container config, List<ModelNode> list) throws Exception {

        configureInterfaces(config, list);
        configureSocketBindingGroups(config, list);

        if(xmlConfig.isPresent())
            configureFractionsFromXML(config, list);
        else
            configureFractions(config, list);

    }

    private void configureInterfaces(Container config, List<ModelNode> list) {
        List<Interface> ifaces = config.ifaces();

        for (Interface each : ifaces) {
            configureInterface(each, list);
        }
    }

    private void configureInterface(Interface iface, List<ModelNode> list) {
        ModelNode node = new ModelNode();

        node.get(OP).set(ADD);
        node.get(OP_ADDR).set("interface", iface.getName());
        node.get(INET_ADDRESS).set(new ValueExpression(iface.getExpression()));

        list.add(node);
    }

    private void configureSocketBindingGroups(Container config, List<ModelNode> list) {
        List<SocketBindingGroup> groups = config.socketBindingGroups();

        for (SocketBindingGroup each : groups) {
            configureSocketBindingGroup(each, list);
        }
    }

    private void configureSocketBindingGroup(SocketBindingGroup group, List<ModelNode> list) {
        ModelNode node = new ModelNode();

        PathAddress address = PathAddress.pathAddress("socket-binding-group", group.name());
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address.toModelNode());
        node.get(DEFAULT_INTERFACE).set(group.defaultInterface());
        node.get(PORT_OFFSET).set(new ValueExpression(group.portOffsetExpression()));
        list.add(node);

        configureSocketBindings(address, group, list);

    }

    private void configureSocketBindings(PathAddress address, SocketBindingGroup group, List<ModelNode> list) {
        List<SocketBinding> bindings = group.socketBindings();

        for (SocketBinding each : bindings) {
            configureSocketBinding(address, each, list);
        }
    }

    private void configureSocketBinding(PathAddress address, SocketBinding binding, List<ModelNode> list) {

        ModelNode node = new ModelNode();

        node.get(OP_ADDR).set(address.append("socket-binding", binding.name()).toModelNode());
        node.get(OP).set(ADD);
        node.get(PORT).set(new ValueExpression(binding.portExpression()));
        if (binding.multicastAddress() != null) {
            node.get(MULTICAST_ADDRESS).set(binding.multicastAddress());
        }
        if (binding.multicastPortExpression() != null) {
            node.get(MULTICAST_PORT).set(new ValueExpression(binding.multicastPortExpression()));
        }

        list.add(node);
    }

    @SuppressWarnings("unchecked")
    private void configureFractionsFromXML(Container container, List<ModelNode> operationList) throws Exception {

        StandaloneXmlParser2 parser = new StandaloneXmlParser2();

        FractionProcessor<StandaloneXmlParser2> consumer = (p, cfg, fraction) -> {
            try {
                if(cfg.getSubsystemParsers().isPresent())
                {
                    Map<QName, XMLElementReader<List<ModelNode>>> fractionParsers =
                            (Map<QName, XMLElementReader<List<ModelNode>>>) cfg.getSubsystemParsers().get();

                    fractionParsers.forEach(p::addDelegate);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // collect parsers
        visitFractions(container, parser, consumer);

        // parse the configurations
        List<ModelNode> parseResult = parser.parse(xmlConfig.get());
        operationList.addAll(parseResult);

    }

    private void configureFractions(Container config, List<ModelNode> list) throws Exception {

        FractionProcessor<List<ModelNode>> consumer = (context, cfg, fraction) -> {
            try {
                context.addAll(cfg.getList(fraction));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        visitFractions(config, list, consumer);
    }

    /**
     * Wraps common iteration pattern over fraction and server configurations
     * @param container
     * @param context processing context (i.e. accumulator)
     * @param fn a {@link org.wildfly.swarm.container.runtime.RuntimeServer.FractionProcessor} instance
     */
    private <T> void visitFractions(Container container, T context, FractionProcessor<T> fn) {
        OUTER:
        for (ServerConfiguration eachConfig : this.configList) {
            boolean found = false;
            INNER:
            for (Fraction eachFraction : container.fractions()) {
                if (eachConfig.getType().isAssignableFrom(eachFraction.getClass())) {
                    found = true;
                    fn.accept(context, eachConfig, eachFraction);
                    break INNER;
                }
            }
            if (!found && !eachConfig.isIgnorable()) {
                System.err.println("*** unable to find fraction for: " + eachConfig.getType());
            }

        }
    }

    @FunctionalInterface
    interface FractionProcessor<T> {
        void accept(T t, ServerConfiguration config, Fraction fraction);
    }


}
