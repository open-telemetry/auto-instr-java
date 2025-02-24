/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jmx.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.instrumentation.jmx.engine.MetricAttribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class MetricStructureTest {

  @ParameterizedTest
  @CsvSource({"const(Hello),Hello", "lowercase(const(Hello)),hello"})
  void metricAttribute_constant(String target, String expectedValue) {
    MetricAttribute ma = MetricStructure.buildMetricAttribute("name", target);
    assertThat(ma.getAttributeName()).isEqualTo("name");
    assertThat(ma.isStateAttribute()).isFalse();
    assertThat(ma.acquireAttributeValue(null, null)).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @CsvSource({
    "beanattr(beanAttribute),Hello,Hello",
    "lowercase(beanattr(beanAttribute)),Hello,hello",
  })
  void metricAttribute_beanAttribute(String target, String value, String expectedValue)
      throws Exception {
    MetricAttribute ma = MetricStructure.buildMetricAttribute("name", target);
    assertThat(ma.getAttributeName()).isEqualTo("name");
    assertThat(ma.isStateAttribute()).isFalse();

    ObjectName objectName = new ObjectName("test:name=_beanAttribute");
    MBeanServerConnection mockConnection = mock(MBeanServerConnection.class);

    MBeanInfo mockBeanInfo = mock(MBeanInfo.class);
    when(mockBeanInfo.getAttributes())
        .thenReturn(
            new MBeanAttributeInfo[] {
              new MBeanAttributeInfo("beanAttribute", "java.lang.String", "", true, false, false)
            });
    when(mockConnection.getMBeanInfo(objectName)).thenReturn(mockBeanInfo);
    when(mockConnection.getAttribute(objectName, "beanAttribute")).thenReturn(value);

    assertThat(ma.acquireAttributeValue(mockConnection, objectName)).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @CsvSource({
    "param(name),Hello,Hello",
    "lowercase(param(name)),Hello,hello",
  })
  void metricAttribute_beanParam(String target, String value, String expectedValue)
      throws Exception {
    MetricAttribute ma = MetricStructure.buildMetricAttribute("name", target);
    assertThat(ma.getAttributeName()).isEqualTo("name");
    assertThat(ma.isStateAttribute()).isFalse();

    ObjectName objectName = new ObjectName("test:name=" + value);
    MBeanServerConnection mockConnection = mock(MBeanServerConnection.class);

    assertThat(ma.acquireAttributeValue(mockConnection, objectName)).isEqualTo(expectedValue);
  }
}
