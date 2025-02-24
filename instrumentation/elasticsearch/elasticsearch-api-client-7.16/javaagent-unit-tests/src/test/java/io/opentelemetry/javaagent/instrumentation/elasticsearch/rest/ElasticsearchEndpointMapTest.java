/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.ElasticsearchEndpointDefinition;
import io.opentelemetry.javaagent.instrumentation.elasticsearch.apiclient.ElasticsearchEndpointMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ElasticsearchEndpointMapTest {

  private static final Set<String> SEARCH_ENDPOINTS =
      new HashSet<>(
          Arrays.asList(
              "search",
              "async_search.submit",
              "msearch",
              "eql.search",
              "terms_enum",
              "search_template",
              "msearch_template",
              "render_search_template"));

  private static List<String> getPathParts(String route) {
    List<String> pathParts = new ArrayList<>();
    String routeFragment = route;
    int paramStartIndex = routeFragment.indexOf('{');
    while (paramStartIndex >= 0) {
      int paramEndIndex = routeFragment.indexOf('}');
      if (paramEndIndex < 0 || paramEndIndex <= paramStartIndex + 1) {
        throw new IllegalStateException("Invalid route syntax!");
      }
      pathParts.add(routeFragment.substring(paramStartIndex + 1, paramEndIndex));

      int nextIdx = paramEndIndex + 1;
      if (nextIdx >= routeFragment.length()) {
        break;
      }

      routeFragment = routeFragment.substring(nextIdx);
      paramStartIndex = routeFragment.indexOf('{');
    }
    return pathParts;
  }

  @Test
  public void testIsSearchEndpoint() {
    for (ElasticsearchEndpointDefinition esEndpointDefinition :
        ElasticsearchEndpointMap.getAllEndpoints()) {
      String endpointId = esEndpointDefinition.getEndpointName();
      assertEquals(SEARCH_ENDPOINTS.contains(endpointId), esEndpointDefinition.isSearchEndpoint());
    }
  }

  @Test
  public void testProcessPathParts() {
    for (ElasticsearchEndpointDefinition esEndpointDefinition :
        ElasticsearchEndpointMap.getAllEndpoints()) {
      for (String route :
          esEndpointDefinition.getRoutes().stream()
              .map(ElasticsearchEndpointDefinition.Route::getName)
              .collect(Collectors.toList())) {
        List<String> pathParts = getPathParts(route);
        String resolvedRoute = route.replace("{", "").replace("}", "");
        Map<String, String> observedParams = new HashMap<>();
        esEndpointDefinition.processPathParts(resolvedRoute, (k, v) -> observedParams.put(k, v));

        Map<String, String> expectedMap = new HashMap<>();
        pathParts.forEach(part -> expectedMap.put(part, part));

        assertEquals(expectedMap, observedParams);
      }
    }
  }

  @Test
  public void testSearchEndpoint() {
    ElasticsearchEndpointDefinition esEndpoint = ElasticsearchEndpointMap.get("search");
    Map<String, String> observedParams = new HashMap<>();
    esEndpoint.processPathParts(
        "/test-index-1,test-index-2/_search", (k, v) -> observedParams.put(k, v));

    assertEquals("test-index-1,test-index-2", observedParams.get("index"));
  }

  @Test
  public void testBuildRegexPattern() {
    Pattern pattern =
        ElasticsearchEndpointDefinition.EndpointPattern.buildRegexPattern(
            "/_nodes/{node_id}/shutdown");
    assertEquals("^/_nodes/(?<node0id>[^/]+)/shutdown$", pattern.pattern());

    pattern =
        ElasticsearchEndpointDefinition.EndpointPattern.buildRegexPattern(
            "/_snapshot/{repository}/{snapshot}/_mount");
    assertEquals("^/_snapshot/(?<repository>[^/]+)/(?<snapshot>[^/]+)/_mount$", pattern.pattern());

    pattern =
        ElasticsearchEndpointDefinition.EndpointPattern.buildRegexPattern(
            "/_security/profile/_suggest");
    assertEquals("^/_security/profile/_suggest$", pattern.pattern());

    pattern =
        ElasticsearchEndpointDefinition.EndpointPattern.buildRegexPattern(
            "/_application/search_application/{name}");
    assertEquals("^/_application/search_application/(?<name>[^/]+)$", pattern.pattern());

    pattern = ElasticsearchEndpointDefinition.EndpointPattern.buildRegexPattern("/");
    assertEquals("^/$", pattern.pattern());
  }
}
