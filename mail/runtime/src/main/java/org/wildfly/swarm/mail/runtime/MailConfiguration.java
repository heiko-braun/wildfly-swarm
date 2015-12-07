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
package org.wildfly.swarm.mail.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.swarm.config.Mail;
import org.wildfly.swarm.config.mail.MailSession;
import org.wildfly.swarm.config.mail.mail_session.SMTPServer;
import org.wildfly.swarm.config.runtime.invocation.Marshaller;
import org.wildfly.swarm.container.runtime.AbstractServerConfiguration;
import org.wildfly.swarm.mail.MailFraction;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author Ken Finnigan
 */
public class MailConfiguration extends AbstractServerConfiguration<MailFraction> {

    private PathAddress smtpServerAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "mail"));

    public MailConfiguration() {
        super(MailFraction.class);
    }

    @Override
    public MailFraction defaultFraction() {
        return new MailFraction();
    }

    @Override
    public List<ModelNode> getList(MailFraction fraction) throws Exception {

        List<ModelNode> list = new ArrayList<>();

        Mail mail = new Mail();
        List<ModelNode> socketBindings = addSmtpServers(fraction, mail);

        list.addAll(Marshaller.marshal(mail));
        list.addAll(socketBindings);

        return list;
    }

    @Override
    public Optional<ModelNode> getExtension() {
        ModelNode node = new ModelNode();
        node.get(OP_ADDR).add(EXTENSION, "org.jboss.as.mail");
        node.get(OP).set(ADD);

        return Optional.of(node);
    }

    protected List<ModelNode> addSmtpServers(MailFraction fraction, Mail mail) {
        List<ModelNode> list = new ArrayList<>();
        for (org.wildfly.swarm.mail.SmtpServer each : fraction.smtpServers()) {
            list.add(addSmtpServer(each, mail));
        }

        return list;
    }

    protected ModelNode addSmtpServer(org.wildfly.swarm.mail.SmtpServer smtpServer, Mail mail) {

        SMTPServer smtp = new SMTPServer().outboundSocketBindingRef(smtpServer.outboundSocketBindingRef());

        MailSession mailSession = new MailSession(smtpServer.name().toLowerCase())
                .smtpServer(smtp)
                .jndiName(smtpServer.jndiName());

        mail.mailSession(mailSession);


        ModelNode node = new ModelNode();
        node.get(OP_ADDR).set(PathAddress.pathAddress("socket-binding-group", "default-sockets").append("remote-destination-outbound-socket-binding", smtpServer.outboundSocketBindingRef()).toModelNode());
        node.get(OP).set(ADD);
        node.get("host").set(smtpServer.host());
        node.get("port").set(smtpServer.port());
        return node;
    }
}
