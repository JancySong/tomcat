/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.coyote.ajp;

import javax.net.ssl.SSLEngine;

import org.apache.coyote.Processor;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.Nio2Channel;
import org.apache.tomcat.util.net.Nio2Endpoint;
import org.apache.tomcat.util.net.Nio2Endpoint.Handler;
import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SocketWrapperBase;


/**
 * This the NIO2 based protocol handler implementation for AJP.
 */
public class AjpNio2Protocol extends AbstractAjpProtocol<Nio2Channel> {

    private static final Log log = LogFactory.getLog(AjpNio2Protocol.class);

    @Override
    protected Log getLog() { return log; }


    @Override
    protected AbstractEndpoint.Handler<Nio2Channel> getHandler() {
        return cHandler;
    }


    // ------------------------------------------------------------ Constructor

    public AjpNio2Protocol() {
        super(new Nio2Endpoint());
        cHandler = new AjpConnectionHandler(this);
        ((Nio2Endpoint) getEndpoint()).setHandler(cHandler);
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * Connection handler for AJP.
     */
    private final AjpConnectionHandler cHandler;


    // ----------------------------------------------------- JMX related methods

    @Override
    protected String getNamePrefix() {
        return ("ajp-nio2");
    }


    // --------------------------------------  AjpConnectionHandler Inner Class

    protected static class AjpConnectionHandler
            extends AbstractAjpConnectionHandler<Nio2Channel>
            implements Handler {

        public AjpConnectionHandler(AjpNio2Protocol proto) {
            super(proto);
        }

        @Override
        protected Log getLog() {
            return log;
        }

        @Override
        public SSLImplementation getSslImplementation() {
            // AJP does not support SSL
            return null;
        }

        /**
         * Expected to be used by the Poller to release resources on socket
         * close, errors etc.
         */
        @Override
        public void release(SocketWrapperBase<Nio2Channel> socket) {
            Processor<Nio2Channel> processor =
                    connections.remove(socket.getSocket());
            if (processor != null) {
                processor.recycle(true);
                recycledProcessors.push(processor);
            }
        }

        /**
         * Expected to be used by the handler once the processor is no longer
         * required.
         */
        @Override
        public void release(SocketWrapperBase<Nio2Channel> socket,
                Processor<Nio2Channel> processor, boolean isSocketClosing,
                boolean addToPoller) {
            if (getLog().isDebugEnabled()) {
                log.debug("Socket: [" + socket + "], Processor: [" + processor +
                        "], isSocketClosing: [" + isSocketClosing +
                        "], addToPoller: [" + addToPoller + "]");
            }
            processor.recycle(isSocketClosing);
            recycledProcessors.push(processor);
        }

        @Override
        public void onCreateSSLEngine(SSLEngine engine) {
        }

        @Override
        public void closeAll() {
            for (Nio2Channel channel : connections.keySet()) {
                ((Nio2Endpoint) getProtocol().getEndpoint()).closeSocket(channel.getSocket());
            }
        }
    }
}
