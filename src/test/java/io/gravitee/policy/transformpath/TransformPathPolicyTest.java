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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.el.SpelTemplateEngine;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.transformpath.configuration.PathChange;
import io.gravitee.policy.transformpath.configuration.TransformPathPolicyConfiguration;
import io.gravitee.reporter.api.http.RequestMetrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TransformPathPolicyTest {

    private TransformPathPolicy transformPathPolicy;
    
    @Mock
    private TransformPathPolicyConfiguration transformPathPolicyConfiguration;

    @Mock
    protected Request request;

    @Mock
    protected Response response;

    @Mock
    protected PolicyChain policyChain;

    @Mock
    protected ExecutionContext executionContext;

    @Before
    public void init() {
        initMocks(this);

        transformPathPolicy = new TransformPathPolicy(transformPathPolicyConfiguration);
        when(request.metrics()).thenReturn(RequestMetrics.on(System.currentTimeMillis()).build());
    }

    @Test
    public void test_shouldThrowFailure_noPathChange() {
        // Prepare inbound request
        final HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/products/ecom/");

        // Prepare context
        when(executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH)).thenReturn("/products");

        // Execute policy
        transformPathPolicy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
        verify(executionContext, never()).setAttribute(any(), any());
    }

    @Test
    public void test_shouldTransformPath_noMatchingPathChange() {
        // Prepare policy configuration
        List<PathChange> pathChanges = new ArrayList<>();
        pathChanges.add(new PathChange(Pattern.compile("/mag/"), "http://host1/product"));

        when(transformPathPolicyConfiguration.getPathChanges()).thenReturn(pathChanges);

        // Prepare inbound request
        final HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/products/v1/ecom/");

        // Prepare context
        when(executionContext.getTemplateEngine()).thenReturn(new SpelTemplateEngine());
        when(executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH)).thenReturn("/products");

        // Execute policy
        transformPathPolicy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
        verify(executionContext, never()).setAttribute(any(), any());
    }

    @Test
    public void test_shouldTransformPath_singleMatchingPathChange() {
        // Prepare policy configuration
        List<PathChange> pathChanges = new ArrayList<>();
        pathChanges.add(new PathChange(Pattern.compile("/v1/ecom/"), "/product"));

        when(transformPathPolicyConfiguration.getPathChanges()).thenReturn(pathChanges);

        // Prepare inbound request
        final HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/products/v1/ecom/");

        // Prepare context
        when(executionContext.getTemplateEngine()).thenReturn(new SpelTemplateEngine());
        when(executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH)).thenReturn("/products");

        // Execute policy
        transformPathPolicy.onRequest(request, response, executionContext, policyChain);

        // Check results
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(policyChain).doNext(argument.capture(), any(Response.class));
        assertEquals("/product", argument.getValue().pathInfo());
        verify(executionContext).setAttribute(eq(ExecutionContext.ATTR_INVOKER), any());
    }

    @Test
    public void test_shouldTransformPath_multipleMatchingPathChange() {
        // Prepare policy configuration
        List<PathChange> pathChanges = new ArrayList<>();
        pathChanges.add(new PathChange(Pattern.compile("/v1/ecom/"), "product1"));
        pathChanges.add(new PathChange(Pattern.compile("/v1/ecom/subpath"), "product2"));

        when(transformPathPolicyConfiguration.getPathChanges()).thenReturn(pathChanges);

        // Prepare inbound request
        final HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/products/v1/ecom/");

        // Prepare context
        when(executionContext.getTemplateEngine()).thenReturn(new SpelTemplateEngine());
        when(executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH)).thenReturn("/products");

        // Execute policy
        transformPathPolicy.onRequest(request, response, executionContext, policyChain);

        // Check results
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(policyChain).doNext(argument.capture(), any(Response.class));
        assertEquals("/product1", argument.getValue().pathInfo());
        verify(executionContext).setAttribute(eq(ExecutionContext.ATTR_INVOKER), any());
    }

    @Test
    public void test_shouldTransformPath_multipleMatchingPathChange_regex() {
        // Prepare policy configuration
        List<PathChange> pathChanges = new ArrayList<>();
        pathChanges.add(new PathChange(Pattern.compile("/v1/ecome.*"), "/product1"));
        pathChanges.add(new PathChange(Pattern.compile("/v1/ecom/(.*)"), "/product2"));

        when(transformPathPolicyConfiguration.getPathChanges()).thenReturn(pathChanges);

        // Prepare inbound request
        final HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/products/v1/ecom/");

        // Prepare context
        when(executionContext.getTemplateEngine()).thenReturn(new SpelTemplateEngine());
        when(executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH)).thenReturn("/products");

        // Execute policy
        transformPathPolicy.onRequest(request, response, executionContext, policyChain);

        // Check results
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(policyChain).doNext(argument.capture(), any(Response.class));
        assertEquals("/product2", argument.getValue().pathInfo());
        verify(executionContext).setAttribute(eq(ExecutionContext.ATTR_INVOKER), any());
    }

    @Test
    public void test_shouldTransformPath_multipleMatchingPathChange_transformEndpoint() {
        // Prepare policy configuration
        List<PathChange> pathChanges = new ArrayList<>();
        pathChanges.add(new PathChange(Pattern.compile("/v1/ecome.*"), "/product"));
        pathChanges.add(new PathChange(Pattern.compile("/v1/ecom/(.*)"), "/product/{#group[0]}"));

        when(transformPathPolicyConfiguration.getPathChanges()).thenReturn(pathChanges);

        // Prepare inbound request
        final HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/products/v1/ecom/search");

        // Prepare context
        when(executionContext.getTemplateEngine()).thenReturn(new SpelTemplateEngine());
        when(executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH)).thenReturn("/products");

        // Execute policy
        transformPathPolicy.onRequest(request, response, executionContext, policyChain);

        // Check results
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(policyChain).doNext(argument.capture(), any(Response.class));
        assertEquals("/product/search", argument.getValue().pathInfo());
        verify(executionContext).setAttribute(eq(ExecutionContext.ATTR_INVOKER), any());
    }

    @Test
    public void test_shouldTransformPath_multipleMatchingPathChange_transformEndpointWithGroupName() {
        // Prepare policy configuration
        List<PathChange> pathChanges = new ArrayList<>();
        pathChanges.add(new PathChange(Pattern.compile("/api/(?<version>v[0-9]+)/ecome.*"), "/products/api/{#groupName['version']}/{#group[0]}"));

        when(transformPathPolicyConfiguration.getPathChanges()).thenReturn(pathChanges);

        // Prepare inbound request
        final HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/products/api/v12/ecome");

        // Prepare context
        when(executionContext.getTemplateEngine()).thenReturn(new SpelTemplateEngine());
        when(executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH)).thenReturn("/products");

        // Execute policy
        transformPathPolicy.onRequest(request, response, executionContext, policyChain);

        // Check results
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(policyChain).doNext(argument.capture(), any(Response.class));
        assertEquals("/products/api/v12/v12", argument.getValue().pathInfo());
        verify(executionContext).setAttribute(eq(ExecutionContext.ATTR_INVOKER), any());
    }
}
