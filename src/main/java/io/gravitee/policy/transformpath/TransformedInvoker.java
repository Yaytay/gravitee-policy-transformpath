/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.transformpath;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.stream.ReadStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class TransformedInvoker implements Invoker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TransformedInvoker.class);
    
    private final Invoker invoker;
    private final String path;

    public TransformedInvoker(Invoker invoker, String path) {
        this.invoker = invoker;
        this.path = path;
    }

    @Override
    public Request invoke(ExecutionContext ec, Request rqst, ReadStream<Buffer> stream, Handler<ProxyConnection> hndlr) {
        LOGGER.debug("Invoking with new path {}", path);
        return invoker.invoke(ec, new TransformedRequest(rqst, path), stream, hndlr);
    }
    
}
