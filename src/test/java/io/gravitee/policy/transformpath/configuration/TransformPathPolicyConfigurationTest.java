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
package io.gravitee.policy.transformpath.configuration;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformPathPolicyConfigurationTest {

    @Test
    public void testConfiguration_singleRule() throws IOException {
        TransformPathPolicyConfiguration configuration =
                load("/io/gravitee/policy/transformpath/configuration/configuration1.json");

        Assert.assertEquals(1, configuration.getPathChanges().size());
    }

    @Test(expected = JsonMappingException.class)
    public void testConfiguration_invalidPattern() throws IOException {
        load("/io/gravitee/policy/transformpath/configuration/configuration2.json");
    }

    private TransformPathPolicyConfiguration load(String resource) throws IOException {
        URL jsonFile = this.getClass().getResource(resource);
        return objectMapper().readValue(jsonFile, TransformPathPolicyConfiguration.class);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
