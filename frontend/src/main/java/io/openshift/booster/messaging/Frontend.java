/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openshift.booster.messaging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@ApplicationPath("/api")
@Path("/")
public class Frontend extends Application {
    private final Data data;

    public Frontend() {
        this.data = new Data();
    }
    
    @GET
    @Path("data")
    @Produces(MediaType.APPLICATION_JSON)
    public Data getData() {
        return data;
    }

    @POST
    @Path("send-request")
    @Consumes(MediaType.APPLICATION_JSON)
    public void sendRequest(Request request) {
        ConnectionFactory factory = lookupConnectionFactory();

        try {
            Connection conn = factory.createConnection();

            conn.start();

            try {
                Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue requestQueue = session.createQueue("requests");
                Queue responseQueue = session.createQueue("responses");
                MessageProducer producer = session.createProducer(requestQueue);
                MessageConsumer consumer = session.createConsumer(responseQueue);

                TextMessage message = session.createTextMessage();

                message.setText(request.getText());
                message.setJMSReplyTo(responseQueue);

                producer.send(message);
            } finally {
                conn.close();
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConnectionFactory lookupConnectionFactory() {
        try {
            InitialContext context = new InitialContext();

            try {
                return (ConnectionFactory) context.lookup("java:global/jms/default");
            } finally {
                context.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}