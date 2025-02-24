/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jmx.yaml;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.instrumentation.jmx.engine.MetricAttribute;
import io.opentelemetry.instrumentation.jmx.engine.MetricAttributeExtractor;
import io.opentelemetry.instrumentation.jmx.engine.MetricInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An abstract class containing skeletal info about Metrics:
 * <li>the metric type
 * <li>the metric attributes
 * <li>the unit
 *
 *     <p>Known subclasses are {@link JmxRule} and {@link Metric}.
 */
abstract class MetricStructure {

  // Used by the YAML parser
  //
  //    type: TYPE
  //    metricAttribute:
  //      KEY1: SPECIFICATION1
  //      KEY2: SPECIFICATION2
  //    unit: UNIT
  //
  // For state metrics
  //
  //    type: state
  //    metricAttribute:
  //      KEY1: SPECIFICATION1
  //      state:
  //        state1: [a,b]
  //        state2: c
  //        state3: '*'

  private Map<String, Object> metricAttribute;
  private StateMapping stateMapping = StateMapping.empty();
  private static final String STATE_MAPPING_DEFAULT = "*";
  private String unit;

  private MetricInfo.Type metricType;
  private List<MetricAttribute> metricAttributes;

  MetricStructure() {}

  void setType(String t) {
    // Do not complain about case variations
    t = t.trim().toUpperCase(Locale.ROOT);
    this.metricType = MetricInfo.Type.valueOf(t);
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = validateUnit(unit.trim());
  }

  private static void addMappedValue(
      StateMapping.Builder builder, String stateValue, String stateKey) {
    if (stateValue.equals(STATE_MAPPING_DEFAULT)) {
      builder.withDefaultState(stateKey);
    } else {
      builder.withMappedValue(stateValue, stateKey);
    }
  }

  @CanIgnoreReturnValue
  private String validateUnit(String unit) {
    requireNonEmpty(unit, "Metric unit is empty");
    return unit;
  }

  /**
   * When the YAML parser sets the metric attributes, convert them immediately to MetricAttribute
   * objects. Any errors during conversion will show in the context of the parsed YAML file.
   *
   * @param map the mapping of metric attribute keys to evaluating snippets
   */
  public void setMetricAttribute(Map<String, Object> map) {
    this.metricAttribute = map;
    // pre-build the MetricAttributes
    this.metricAttributes = addMetricAttributes(map);
  }

  // Used only for testing
  public Map<String, Object> getMetricAttribute() {
    return metricAttribute;
  }

  public MetricInfo.Type getMetricType() {
    return metricType;
  }

  protected List<MetricAttribute> getAttributeList() {
    return metricAttributes;
  }

  protected void requireNonEmpty(String s, String msg) {
    if (s.isEmpty()) {
      throw new IllegalArgumentException(msg);
    }
  }

  private List<MetricAttribute> addMetricAttributes(Map<String, Object> metricAttributeMap) {

    List<MetricAttribute> list = new ArrayList<>();
    for (String key : metricAttributeMap.keySet()) {
      Object value = metricAttributeMap.get(key);
      if (value == null) {
        throw new IllegalStateException("nothing specified for metric attribute key '" + key + "'");
      }

      MetricAttribute attribute;
      if (value instanceof String) {
        attribute = buildMetricAttribute(key, ((String) value).trim());
      } else if (value instanceof Map) {
        // here we use the structure to detect a state metric attribute and parse it.
        attribute = buildStateMetricAttribute(key, (Map<?, ?>) value);
      } else {
        throw new IllegalArgumentException("unexpected metric attribute: " + value);
      }

      list.add(attribute);
    }
    return list;
  }

  // package protected for testing
  static MetricAttribute buildMetricAttribute(String key, String target) {

    // an optional modifier may wrap the target
    // - lowercase(param(STRING))
    boolean lowercase = false;
    if (target.startsWith("lowercase(")) {
      lowercase = true;
      target = target.substring(10, target.lastIndexOf(')'));
    }

    //
    // The recognized forms of target are:
    //  - param(STRING)
    //  - beanattr(STRING)
    //  - const(STRING)
    // where STRING is the name of the corresponding parameter key, attribute name,
    // or the direct value to use
    int k = target.indexOf(')');

    MetricAttributeExtractor extractor = null;

    // Check for one of the cases as above
    if (target.startsWith("param(")) {
      if (k > 0) {
        String jmxAttribute = target.substring(6, k).trim();
        extractor = MetricAttributeExtractor.fromObjectNameParameter(jmxAttribute);
      }
    } else if (target.startsWith("beanattr(")) {
      if (k > 0) {
        String jmxAttribute = target.substring(9, k).trim();
        extractor = MetricAttributeExtractor.fromBeanAttribute(jmxAttribute);
      }
    } else if (target.startsWith("const(")) {
      if (k > 0) {
        String constantValue = target.substring(6, k).trim();
        extractor = MetricAttributeExtractor.fromConstant(constantValue);
      }
    }
    if (extractor != null) {
      if (lowercase) {
        extractor = MetricAttributeExtractor.toLowerCase(extractor);
      }

      return new MetricAttribute(key, extractor);
    }

    String msg = "Invalid metric attribute specification for '" + key + "': " + target;
    throw new IllegalArgumentException(msg);
  }

  private MetricAttribute buildStateMetricAttribute(String key, Map<?, ?> stateMap) {
    if (stateMap.isEmpty()) {
      throw new IllegalArgumentException("state map is empty for key " + key);
    }
    if (!stateMapping.isEmpty()) {
      throw new IllegalStateException("only a single state map is expected");
    }

    StateMapping.Builder builder = StateMapping.builder();

    for (Map.Entry<?, ?> entry : stateMap.entrySet()) {
      if (!(entry.getKey() instanceof String)) {
        throw new IllegalArgumentException("unexpected state map key: " + entry.getKey());
      }
      String stateKey = (String) entry.getKey();
      Object objValue = entry.getValue();
      if (objValue instanceof String) {
        addMappedValue(builder, (String) objValue, stateKey);
      } else if (objValue instanceof List) {
        for (Object listEntry : (List<?>) objValue) {
          if (!(listEntry instanceof String)) {
            throw new IllegalArgumentException("unexpected state list value: " + stateKey);
          }
          addMappedValue(builder, (String) listEntry, stateKey);
        }
      }
    }
    stateMapping = builder.build();
    return new MetricAttribute(key, null);
  }

  public StateMapping getStateMapping() {
    return stateMapping;
  }
}
