/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jmx.engine;

// This test is put in the io.opentelemetry.instrumentation.jmx.engine package
// because it needs to access package-private methods from a number of classes.

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.instrumentation.jmx.yaml.JmxConfig;
import io.opentelemetry.instrumentation.jmx.yaml.JmxRule;
import io.opentelemetry.instrumentation.jmx.yaml.Metric;
import io.opentelemetry.instrumentation.jmx.yaml.RuleParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RuleParserTest {
  private static RuleParser parser;

  @BeforeAll
  static void setup() {
    parser = RuleParser.get();
    assertThat(parser).isNotNull();
  }

  /*
   * General syntax
   */
  private static final String CONF2 =
      "---\n"
          + "rules:\n"
          + "  - beans:\n"
          + "      - OBJECT:NAME1=*\n"
          + "      - OBJECT:NAME2=*\n"
          + "    metricAttribute:\n"
          + "      LABEL_KEY1: param(PARAMETER)\n"
          + "      LABEL_KEY2: beanattr(ATTRIBUTE)\n"
          + "    prefix: METRIC_NAME_PREFIX\n"
          + "    mapping:\n"
          + "      ATTRIBUTE1:\n"
          + "        metric: METRIC_NAME1\n"
          + "        type: Gauge\n"
          + "        desc: DESCRIPTION1\n"
          + "        unit: UNIT1\n"
          + "        metricAttribute:\n"
          + "          LABEL_KEY3: const(CONSTANT)\n"
          + "      ATTRIBUTE2:\n"
          + "        metric: METRIC_NAME2\n"
          + "        desc: DESCRIPTION2\n"
          + "        unit: UNIT2\n"
          + "      ATTRIBUTE3:\n"
          + "      ATTRIBUTE4:\n"
          + "  - beans:\n"
          + "      - OBJECT:NAME3=*\n"
          + "    mapping:\n"
          + "      ATTRIBUTE3:\n"
          + "        metric: METRIC_NAME3\n";

  @Test
  void testConf2() {
    JmxConfig config = parseConf(CONF2);

    List<JmxRule> defs = config.getRules();
    assertThat(defs).hasSize(2);

    JmxRule def1 = defs.get(0);
    assertThat(def1.getBeans()).containsExactly("OBJECT:NAME1=*", "OBJECT:NAME2=*");
    assertThat(def1.getMetricAttribute())
        .hasSize(2)
        .containsEntry("LABEL_KEY1", "param(PARAMETER)")
        .containsEntry("LABEL_KEY2", "beanattr(ATTRIBUTE)");

    Map<String, Metric> attr = def1.getMapping();
    assertThat(attr)
        .hasSize(4)
        .containsKeys("ATTRIBUTE1", "ATTRIBUTE2", "ATTRIBUTE3", "ATTRIBUTE4");

    Metric m1 = attr.get("ATTRIBUTE1");
    assertThat(m1).isNotNull();
    assertThat(m1.getMetric()).isEqualTo("METRIC_NAME1");
    assertThat(m1.getMetricType()).isEqualTo(MetricInfo.Type.GAUGE);
    assertThat(m1.getUnit()).isEqualTo("UNIT1");
    assertThat(m1.getMetricAttribute()).containsExactly(entry("LABEL_KEY3", "const(CONSTANT)"));

    Metric m2 = attr.get("ATTRIBUTE2");
    assertThat(m2).isNotNull();
    assertThat(m2.getMetric()).isEqualTo("METRIC_NAME2");
    assertThat(m2.getDesc()).isEqualTo("DESCRIPTION2");
    assertThat(m2.getUnit()).isEqualTo("UNIT2");

    JmxRule def2 = defs.get(1);
    assertThat(def2.getBeans()).containsExactly("OBJECT:NAME3=*");
    assertThat(def2.getMetricAttribute()).isNull();

    assertThat(def2.getMapping()).hasSize(1);
    Metric m3 = def2.getMapping().get("ATTRIBUTE3");
    assertThat(m3.getMetric()).isEqualTo("METRIC_NAME3");
    assertThat(m3.getUnit()).isNull();
  }

  private static final String CONF3 =
      "rules:\n"
          + "  - bean: OBJECT:NAME3=*\n"
          + "    mapping:\n"
          + "      ATTRIBUTE31:\n"
          + "      ATTRIBUTE32:\n"
          + "      ATTRIBUTE33:\n"
          + "      ATTRIBUTE34:\n"
          + "        metric: METRIC_NAME34\n"
          + "      ATTRIBUTE35:\n";

  @Test
  void testConf3() {
    JmxConfig config = parseConf(CONF3);

    List<JmxRule> defs = config.getRules();
    assertThat(defs).hasSize(1);

    JmxRule def1 = defs.get(0);
    assertThat(def1.getBean()).isNotNull();
    assertThat(def1.getMetricAttribute()).isNull();

    Map<String, Metric> attr = def1.getMapping();
    assertThat(attr)
        .hasSize(5)
        .containsKeys("ATTRIBUTE31", "ATTRIBUTE32", "ATTRIBUTE33", "ATTRIBUTE34", "ATTRIBUTE35");
    assertThat(attr.get("ATTRIBUTE32")).isNull();
    assertThat(attr.get("ATTRIBUTE33")).isNull();
    Metric attribute34 = attr.get("ATTRIBUTE34");
    assertThat(attribute34).isNotNull();
    assertThat(attribute34.getMetric()).isEqualTo("METRIC_NAME34");
    assertThat(attr.get("ATTRIBUTE35")).isNull();
  }

  /*
   * Semantics
   */
  private static final String CONF4 =
      "---\n"
          + "rules:\n"
          + "  - bean: my-test:type=4\n"
          + "    metricAttribute:\n"
          + "      LABEL_KEY1: param(PARAMETER)\n"
          + "      LABEL_KEY2: beanattr(ATTRIBUTE)\n"
          + "    prefix: PREFIX.\n"
          + "    type: upDownCounter\n"
          + "    unit: DEFAULT_UNIT\n"
          + "    mapping:\n"
          + "      A.b:\n"
          + "        metric: METRIC_NAME1\n"
          + "        type: counter\n"
          + "        desc: DESCRIPTION1\n"
          + "        unit: UNIT1\n"
          + "        metricAttribute:\n"
          + "          LABEL_KEY3: const(CONSTANT)\n"
          + "      ATTRIBUTE2:\n"
          + "        metric: METRIC_NAME2\n"
          + "        desc: DESCRIPTION2\n"
          + "        unit: UNIT2\n"
          + "      ATTRIBUTE3:\n";

  @Test
  void testConf4() throws Exception {
    JmxConfig config = parseConf(CONF4);

    List<JmxRule> defs = config.getRules();
    assertThat(defs).hasSize(1);

    JmxRule jmxDef = defs.get(0);
    assertThat(jmxDef.getUnit()).isEqualTo("DEFAULT_UNIT");
    assertThat(jmxDef.getMetricType()).isEqualTo(MetricInfo.Type.UPDOWNCOUNTER);

    MetricDef metricDef = jmxDef.buildMetricDef();
    assertThat(metricDef).isNotNull();

    assertThat(metricDef.getMetricExtractors())
        .hasSize(3)
        .anySatisfy(
            m -> {
              assertThat(m.getMetricValueExtractor().getAttributeName()).isEqualTo("A.b");
              assertThat(m.getAttributes())
                  .hasSize(3)
                  .extracting("attributeName")
                  .contains("LABEL_KEY1", "LABEL_KEY2", "LABEL_KEY3");

              MetricInfo metricInfo = m.getInfo();
              assertThat(metricInfo.getMetricName()).isEqualTo("PREFIX.METRIC_NAME1");
              assertThat(metricInfo.getDescription()).isEqualTo("DESCRIPTION1");
              assertThat(metricInfo.getUnit()).isEqualTo("UNIT1");
              assertThat(metricInfo.getType()).isEqualTo(MetricInfo.Type.COUNTER);
            })
        .anySatisfy(
            m -> {
              assertThat(m.getMetricValueExtractor().getAttributeName()).isEqualTo("ATTRIBUTE2");
              assertThat(m.getAttributes())
                  .hasSize(2)
                  .extracting("attributeName")
                  .contains("LABEL_KEY1", "LABEL_KEY2");

              MetricInfo metricInfo = m.getInfo();
              assertThat(metricInfo.getMetricName()).isEqualTo("PREFIX.METRIC_NAME2");
              assertThat(metricInfo.getDescription()).isEqualTo("DESCRIPTION2");
              assertThat(metricInfo.getUnit()).isEqualTo("UNIT2");
            })
        .anySatisfy(
            m -> {
              assertThat(m.getMetricValueExtractor().getAttributeName()).isEqualTo("ATTRIBUTE3");

              MetricInfo metricInfo = m.getInfo();
              assertThat(metricInfo.getMetricName()).isEqualTo("PREFIX.ATTRIBUTE3");
              assertThat(metricInfo.getDescription()).isNull();

              // syntax extension - defining a default unit and type
              assertThat(metricInfo.getType())
                  .describedAs("default type should match jmx rule definition")
                  .isEqualTo(jmxDef.getMetricType());
              assertThat(metricInfo.getUnit())
                  .describedAs("default unit should match jmx rule definition")
                  .isEqualTo(jmxDef.getUnit());
            });
  }

  private static final String CONF5 = // minimal valid definition
      "---                                   # keep stupid spotlessJava at bay\n"
          + "rules:\n"
          + "  - bean: my-test:type=5\n"
          + "    mapping:\n"
          + "      ATTRIBUTE:\n";

  @Test
  void testConf5() throws Exception {
    JmxConfig config = parseConf(CONF5);

    List<JmxRule> defs = config.getRules();
    assertThat(defs).hasSize(1);

    MetricDef metricDef = defs.get(0).buildMetricDef();
    assertThat(metricDef).isNotNull();
    assertThat(metricDef.getMetricExtractors()).hasSize(1);

    MetricExtractor m1 = metricDef.getMetricExtractors().get(0);
    assertThat(m1.getMetricValueExtractor().getAttributeName()).isEqualTo("ATTRIBUTE");
    assertThat(m1.getAttributes()).isEmpty();

    MetricInfo mb1 = m1.getInfo();
    assertThat(mb1.getMetricName()).isEqualTo("ATTRIBUTE");
    assertThat(mb1.getType()).isEqualTo(MetricInfo.Type.GAUGE);
    assertThat(mb1.getUnit()).isNull();
  }

  private static final String CONF6 = // merging metric attribute sets with same keys
      "---                                   # keep stupid spotlessJava at bay\n"
          + "rules:\n"
          + "  - bean: my-test:type=6\n"
          + "    metricAttribute:\n"
          + "      key1: const(value1)\n"
          + "    mapping:\n"
          + "      ATTRIBUTE:\n"
          + "        metricAttribute:\n"
          + "          key1: const(value2)\n";

  @Test
  void testConf6() throws Exception {
    JmxConfig config = parseConf(CONF6);

    List<JmxRule> defs = config.getRules();
    assertThat(defs).hasSize(1);

    MetricDef metricDef = defs.get(0).buildMetricDef();
    assertThat(metricDef).isNotNull();
    assertThat(metricDef.getMetricExtractors()).hasSize(1);

    MetricExtractor m1 = metricDef.getMetricExtractors().get(0);
    assertThat(m1.getMetricValueExtractor().getAttributeName()).isEqualTo("ATTRIBUTE");
    // MetricAttribute set at the metric level should override the one set at the definition level
    assertThat(m1.getAttributes())
        .hasSize(1)
        .satisfiesExactly(a -> checkConstantMetricAttribute(a, "key1", "value2"));

    assertThat(m1.getInfo().getMetricName())
        .describedAs("metric name should default to JMX attribute name")
        .isEqualTo("ATTRIBUTE");
  }

  private static final String CONF7 =
      "---                                   # keep stupid spotlessJava at bay\n"
          + "rules:\n"
          + "  - bean: my-test:type=7\n"
          + "    metricAttribute:\n"
          + "      key1: const(value1)\n"
          + "    mapping:\n"
          + "      ATTRIBUTE:\n"
          + "        metricAttribute:\n"
          + "          key2: const(value2)\n";

  @Test
  void testConf7() throws Exception {
    JmxConfig config = parseConf(CONF7);

    List<JmxRule> defs = config.getRules();
    assertThat(defs).hasSize(1);

    MetricDef metricDef = defs.get(0).buildMetricDef();
    assertThat(metricDef).isNotNull();
    assertThat(metricDef.getMetricExtractors()).hasSize(1);

    // Test that the MBean attribute is correctly parsed
    MetricExtractor m1 = metricDef.getMetricExtractors().get(0);
    assertThat(m1.getMetricValueExtractor().getAttributeName()).isEqualTo("ATTRIBUTE");
    assertThat(m1.getInfo().getMetricName()).isEqualTo("ATTRIBUTE");
    assertThat(m1.getAttributes())
        .hasSize(2)
        .anySatisfy(a -> checkConstantMetricAttribute(a, "key1", "value1"))
        .anySatisfy(a -> checkConstantMetricAttribute(a, "key2", "value2"));
  }

  private static final String EMPTY_CONF = "---\n";

  private static final String CONF8 = // a dot in attribute name
      "---                                   # keep stupid spotlessJava at bay\n"
          + "rules:\n"
          + "  - bean: my-test:type=8\n"
          + "    mapping:\n"
          + "      Attr.with\\.dot:\n";

  @Test
  void testConf8() throws Exception {
    JmxConfig config = parseConf(CONF8);

    List<JmxRule> defs = config.getRules();
    assertThat(defs).hasSize(1);

    MetricDef metricDef = defs.get(0).buildMetricDef();
    assertThat(metricDef).isNotNull();
    assertThat(metricDef.getMetricExtractors()).hasSize(1);

    MetricExtractor m1 = metricDef.getMetricExtractors().get(0);
    assertThat(m1.getMetricValueExtractor().getAttributeName()).isEqualTo("Attr.with.dot");
    assertThat(m1.getAttributes()).isEmpty();

    MetricInfo mb1 = m1.getInfo();
    // Make sure the metric name has no backslash
    assertThat(mb1.getMetricName()).isEqualTo("Attr.with.dot");
    assertThat(mb1.getType()).isEqualTo(MetricInfo.Type.GAUGE);
    assertThat(mb1.getUnit()).isNull();
  }

  private static final String CONF9 =
      "---                                   # keep stupid spotlessJava at bay\n"
          + "rules:\n"
          + "  - bean: my-test:type=9\n"
          + "    mapping:\n"
          + "      jmxStateAttribute:\n"
          + "        type: state\n"
          + "        metric: state_metric\n"
          + "        metricAttribute:\n"
          + "          state_attribute: \n" // --> only one state attribute allowed
          + "            ok: STARTED\n" // as simple string
          + "            failed: [STOPPED,FAILED]\n" // as array of strings
          + "            degraded: '*'\n" // degraded value for default
          + "";

  @Test
  void testStateMetricConf() throws Exception {
    JmxConfig config = parseConf(CONF9);

    List<JmxRule> rules = config.getRules();
    assertThat(rules).hasSize(1);

    JmxRule jmxRule = rules.get(0);
    assertThat(jmxRule.getBean()).isEqualTo("my-test:type=9");
    Metric metric = jmxRule.getMapping().get("jmxStateAttribute");
    assertThat(metric.getMetricType()).isEqualTo(MetricInfo.Type.STATE);

    assertThat(metric.getStateMapping().isEmpty()).isFalse();
    assertThat(metric.getStateMapping().getStateKeys()).contains("ok", "failed", "degraded");
    assertThat(metric.getStateMapping().getDefaultStateKey()).isEqualTo("degraded");
    assertThat(metric.getStateMapping().getStateValue("STARTED")).isEqualTo("ok");
    assertThat(metric.getStateMapping().getStateValue("STOPPED")).isEqualTo("failed");
    assertThat(metric.getStateMapping().getStateValue("FAILED")).isEqualTo("failed");
    assertThat(metric.getStateMapping().getStateValue("OTHER")).isEqualTo("degraded");

    Map<String, Object> metricAttributeMap = metric.getMetricAttribute();
    assertThat(metricAttributeMap).containsKey("state_attribute").hasSize(1);
    assertThat(metricAttributeMap.get("state_attribute")).isInstanceOf(Map.class);

    ObjectName objectName = new ObjectName(jmxRule.getBean());
    MBeanServerConnection mockConnection = mock(MBeanServerConnection.class);

    // mock attribute value
    when(mockConnection.getAttribute(objectName, "jmxStateAttribute")).thenReturn("STOPPED");

    // mock attribute discovery
    MBeanInfo mockBeanInfo = mock(MBeanInfo.class);
    when(mockBeanInfo.getAttributes())
        .thenReturn(
            new MBeanAttributeInfo[] {
              new MBeanAttributeInfo(
                  "jmxStateAttribute", "java.lang.String", "", true, false, false)
            });
    when(mockConnection.getMBeanInfo(objectName)).thenReturn(mockBeanInfo);

    MetricDef metricDef = jmxRule.buildMetricDef();
    assertThat(metricDef.getMetricExtractors())
        .hasSize(3)
        .allSatisfy(
            me -> {
              assertThat(me.getInfo().getMetricName()).isEqualTo("state_metric");
              assertThat(me.getInfo().getType()).isEqualTo(MetricInfo.Type.UPDOWNCOUNTER);

              assertThat(me.getAttributes()).hasSize(1);
              MetricAttribute stateAttribute = me.getAttributes().get(0);
              assertThat(stateAttribute.getAttributeName()).isEqualTo("state_attribute");
              String stateAttributeValue =
                  stateAttribute.acquireAttributeValue(mockConnection, objectName);

              BeanAttributeExtractor attributeExtractor = me.getMetricValueExtractor();
              assertThat(attributeExtractor).isNotNull();
              assertThat(attributeExtractor.getSampleValue(null, null))
                  .describedAs("sampled value must be an integer")
                  .isInstanceOf(Integer.class);

              assertThat(attributeExtractor.getAttributeInfo(mockConnection, objectName))
                  .describedAs("attribute info must be provided as a regular int metric")
                  .isNotNull();

              int expectedValue = stateAttributeValue.equals("failed") ? 1 : 0;
              Number extractedValue =
                  attributeExtractor.extractNumericalAttribute(mockConnection, objectName);
              assertThat(extractedValue)
                  .describedAs(
                      "metric value should be %d when '%s' attribute is '%s'",
                      expectedValue, stateAttribute.getAttributeName(), stateAttributeValue)
                  .isEqualTo(expectedValue);
            });
  }

  private static final String CONF10 =
      "---                                   # keep stupid spotlessJava at bay\n"
          + "rules:\n"
          + "  - bean: my-test:type=10_Hello\n"
          + "    mapping:\n"
          + "      jmxAttribute:\n"
          + "        type: counter\n"
          + "        metric: my_metric\n"
          + "        metricAttribute:\n"
          + "          to_lower_const: lowercase(const(Hello))\n"
          + "          to_lower_attribute: lowercase(beanattr(beanAttribute))\n"
          + "          to_lower_param: lowercase(param(type))\n";

  @Test
  void attributeValueLowercase() {

    JmxConfig config = parseConf(CONF10);

    List<JmxRule> rules = config.getRules();
    assertThat(rules).hasSize(1);
    JmxRule jmxRule = rules.get(0);

    assertThat(jmxRule.getBean()).isEqualTo("my-test:type=10_Hello");
    Metric metric = jmxRule.getMapping().get("jmxAttribute");
    assertThat(metric.getMetricType()).isEqualTo(MetricInfo.Type.COUNTER);
    assertThat(metric.getMetric()).isEqualTo("my_metric");
    assertThat(metric.getMetricAttribute())
        .hasSize(3)
        .containsEntry("to_lower_const", "lowercase(const(Hello))")
        .containsEntry("to_lower_attribute", "lowercase(beanattr(beanAttribute))")
        .containsEntry("to_lower_param", "lowercase(param(type))");
  }

  @Test
  void testEmptyConf() {
    JmxConfig config = parseConf(EMPTY_CONF);
    assertThat(config.getRules()).isEmpty();
  }

  private static void checkConstantMetricAttribute(
      MetricAttribute attribute, String expectedName, String expectedValue) {
    assertThat(attribute.getAttributeName()).isEqualTo(expectedName);
    assertThat(attribute.acquireAttributeValue(null, null)).isEqualTo(expectedValue);
  }

  private static JmxConfig parseConf(String s) {
    InputStream is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    JmxConfig jmxConfig = parser.loadConfig(is);
    assertThat(jmxConfig).isNotNull();
    return jmxConfig;
  }

  /*
   *     Negative tests
   */

  private static void runNegativeTest(String yaml) {
    Assertions.assertThrows(
        Exception.class,
        () -> {
          JmxConfig config = parseConf(yaml);

          List<JmxRule> defs = config.getRules();
          assertThat(defs).hasSize(1);
          defs.get(0).buildMetricDef();
        });
  }

  @Test
  void testNoBeans() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:                  # no bean\n"
            + "  - mapping:           # still no beans\n"
            + "      A:\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testInvalidObjectName() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:\n"
            + "  - bean: BAD_OBJECT_NAME\n"
            + "    mapping:\n"
            + "      A:\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testEmptyMapping() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n "
            + "rules:\n"
            + "  - bean: domain:type=6\n"
            + "    mapping:\n";
    runNegativeTest(yaml);
  }

  @Test
  void testInvalidAttributeName() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      .used:\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testInvalidTag() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      ATTRIB:\n"
            + "        metricAttribute:\n"
            + "          LABEL: something\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testInvalidType() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      ATTRIB:\n"
            + "        type: gage\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testInvalidTagFromAttribute() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      ATTRIB:\n"
            + "        metricAttribute:\n"
            + "          LABEL: beanattr(.used)\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testEmptyTagFromAttribute() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      ATTRIB:\n"
            + "        metricAttribute:\n"
            + "          LABEL: beanattr( )\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testEmptyTagFromParameter() {
    String yaml =
        "---                                   # keep stupid spotlessJava at bay\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      ATTRIB:\n"
            + "        metricAttribute:\n"
            + "          LABEL: param( )\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testTypoInMetric() {
    String yaml =
        "---\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      A:\n"
            + "        metrics: METRIC_NAME\n";
    runNegativeTest(yaml);
  }

  @Test
  void testMessedUpSyntax() {
    String yaml =
        "---\n"
            + "rules:\n"
            + "  - bean: domain:name=you\n"
            + "    mapping:\n"
            + "      metricAttribute:     # not valid here\n"
            + "        key: const(value)\n"
            + "      A:\n"
            + "        metric: METRIC_NAME\n";
    runNegativeTest(yaml);
  }
}
