/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class ElasticsearchEndpointDefinition {

  private static final String UNDERSCORE_REPLACEMENT = "0";

  private final String endpointName;
  private final List<Route> routes;

  private final boolean isSearchEndpoint;

  public ElasticsearchEndpointDefinition(
      String endpointName, String[] routes, boolean isSearchEndpoint) {
    this.endpointName = endpointName;
    this.routes =
        unmodifiableList(Arrays.stream(routes).map(Route::new).collect(Collectors.toList()));
    this.isSearchEndpoint = isSearchEndpoint;
  }

  @Nullable
  public String getEndpointName() {
    return endpointName;
  }

  public boolean isSearchEndpoint() {
    return isSearchEndpoint;
  }

  public void processPathParts(String urlPath, BiConsumer<String, String> consumer) {
    for (Route route : routes) {
      if (route.hasParameters()) {
        Matcher matcher = route.createMatcher(urlPath);
        if (matcher.find()) {
          for (String key : route.getPathPartNames()) {
            String value = matcher.group(key);
            if (key.contains(UNDERSCORE_REPLACEMENT)) {
              // replace underscore back
              key = key.replace(UNDERSCORE_REPLACEMENT, "_");
            }
            consumer.accept(key, value);
          }
          return;
        }
      }
    }
  }

  public List<Route> getRoutes() {
    return routes;
  }

  /**
   * This class is internal and is hence not for public use. Its APIs are unstable and can change at
   * any time.
   */
  // Visible for testing
  public static final class Route {
    private final String name;
    private final boolean hasParameters;

    private volatile EndpointPattern epPattern;

    public Route(String name) {
      this.name = name;
      this.hasParameters = name.contains("{") && name.contains("}");
    }

    public String getName() {
      return name;
    }

    boolean hasParameters() {
      return hasParameters;
    }

    List<String> getPathPartNames() {
      return getEndpointPattern().getPathPartNames();
    }

    Matcher createMatcher(String urlPath) {
      return getEndpointPattern().getPattern().matcher(urlPath);
    }

    private EndpointPattern getEndpointPattern() {
      // Intentionally NOT synchronizing here to avoid synchronization overhead.
      // Main purpose here is to cache the pattern without the need for strict thread-safety.
      if (epPattern == null) {
        epPattern = new EndpointPattern(this);
      }

      return epPattern;
    }
  }

  /**
   * This class is internal and is hence not for public use. Its APIs are unstable and can change at
   * any time.
   */
  // Visible for testing
  public static final class EndpointPattern {
    private static final Pattern PATH_PART_NAMES_PATTERN = Pattern.compile("\\{([^}]+)}");
    private final Pattern pattern;
    private final List<String> pathPartNames;

    /**
     * Creates, compiles and caches a regular expression pattern and retrieves a set of
     * pathPartNames (names of the URL path parameters) for this route.
     *
     * <p>The regex pattern is later being used to match against a URL path to retrieve the URL path
     * parameters for that route pattern using named regex capture groups.
     */
    private EndpointPattern(Route route) {
      pattern = buildRegexPattern(route.getName());

      if (route.hasParameters()) {
        pathPartNames = new ArrayList<>();
        Matcher matcher = PATH_PART_NAMES_PATTERN.matcher(route.getName());
        while (matcher.find()) {
          String groupName = matcher.group(1);

          if (groupName != null) {
            groupName = groupName.replace("_", UNDERSCORE_REPLACEMENT);
            pathPartNames.add(groupName);
          }
        }
      } else {
        pathPartNames = Collections.emptyList();
      }
    }

    /** Builds a regex pattern from the parameterized route pattern. */
    public static Pattern buildRegexPattern(String routeStr) {
      StringBuilder regexStr = new StringBuilder();
      regexStr.append('^');
      int startIdx = routeStr.indexOf("{");
      while (startIdx >= 0) {
        regexStr.append(routeStr.substring(0, startIdx));

        int endIndex = routeStr.indexOf("}");
        if (endIndex <= startIdx + 1) {
          break;
        }

        // Append named capture group.
        // If group name contains an underscore `_` it is being replaced with `0`,
        // because `_` is not allowed in capture group names.
        regexStr.append("(?<");
        regexStr.append(
            routeStr.substring(startIdx + 1, endIndex).replace("_", UNDERSCORE_REPLACEMENT));
        regexStr.append(">[^/]+)");

        routeStr = routeStr.substring(endIndex + 1);
        startIdx = routeStr.indexOf("{");
      }

      regexStr.append(routeStr);
      regexStr.append('$');

      return Pattern.compile(regexStr.toString());
    }

    Pattern getPattern() {
      return pattern;
    }

    List<String> getPathPartNames() {
      return pathPartNames;
    }
  }
}
