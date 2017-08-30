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
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.transformpath.configuration.PathChange;
import io.gravitee.policy.transformpath.configuration.TransformPathPolicyConfiguration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jim TALBUT (jim.talbut at groupgti.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransformPathPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformPathPolicy.class);

    private final static Pattern GROUP_NAME_PATTERN = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

    private final static String GROUP_ATTRIBUTE = "group";
    private final static String GROUP_NAME_ATTRIBUTE = "groupName";

    /**
     * Transform path configuration
     */
    private final TransformPathPolicyConfiguration configuration;

    public TransformPathPolicy(final TransformPathPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        String path = request.path();
        String contextPath = (String) executionContext.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH);
        String subPath = path.substring(contextPath.length());

        LOGGER.debug("Transforming path from {}", subPath);

        if (configuration.getPathChanges() != null && !configuration.getPathChanges().isEmpty()) {
            // Look for a matching pattern from rules
            Optional<PathChange> optPathChange = configuration.getPathChanges().stream().filter(
                    pathChange -> pathChange.getPattern().matcher(subPath).matches()).findFirst();

            if (optPathChange.isPresent()) {
                PathChange pathChange = optPathChange.get();

                LOGGER.debug("Applying rule for path {}: [{} - {}]", subPath, pathChange.getPattern(), pathChange.getPath());
                String newPath = pathChange.getPath();

                // Apply regex capture / replacement
                Matcher match = pathChange.getPattern().matcher(subPath);

                // Required to calculate capture groups
                match.matches();

                // Extract capture group by index
                String [] groups = new String[match.groupCount()];
                for (int idx = 0; idx < match.groupCount(); idx++) {
                    groups[idx] = match.group(idx + 1);
                }
                executionContext.getTemplateEngine().getTemplateContext().setVariable(GROUP_ATTRIBUTE, groups);

                // Extract capture group by name
                Set<String> extractedGroupNames = getNamedGroupCandidates(pathChange.getPattern().pattern());
                Map<String, String> groupNames = extractedGroupNames.stream().collect(
                        Collectors.toMap(groupName -> groupName, match::group));
                executionContext.getTemplateEngine().getTemplateContext().setVariable(GROUP_NAME_ATTRIBUTE, groupNames);

                // Given endpoint can be defined as the template using EL
                LOGGER.debug("Transform endpoint {} using template engine", newPath);
                newPath = executionContext.getTemplateEngine().convert(newPath);

                LOGGER.debug("Request path updated to {}", newPath);

                // Change the invoker to one that knows the new path
                Invoker invoker = (Invoker) executionContext.getAttribute(ExecutionContext.ATTR_INVOKER);
                invoker = new TransformedInvoker(invoker, newPath);
                executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, invoker);
                
                // And continue request processing....
                policyChain.doNext(new TransformedRequest(request, newPath), response);
                
                return;
            }
        }
        
        // No matchign rules defined
        policyChain.doNext(request, response);
    }

    private Set<String> getNamedGroupCandidates(String regex) {
        Set<String> namedGroups = new TreeSet<>();
        Matcher m = GROUP_NAME_PATTERN.matcher(regex);

        while (m.find()) {
            namedGroups.add(m.group(1));
        }

        return namedGroups;
    }
    
}
