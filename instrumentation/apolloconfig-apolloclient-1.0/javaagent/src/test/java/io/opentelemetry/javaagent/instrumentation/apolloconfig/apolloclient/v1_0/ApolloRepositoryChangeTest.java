/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apolloconfig.apolloclient.v1_0;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.util.Collections.singletonList;

import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.internals.AbstractConfigRepository;
import com.ctrip.framework.apollo.internals.ConfigRepository;
import com.ctrip.framework.apollo.internals.RepositoryChangeListener;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ApolloRepositoryChangeTest {

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void fireRepositoryChangeTest() {
    String namespace = "application";

    TestConfigRepository testConfigRepository = new TestConfigRepository(namespace);
    testConfigRepository.addChangeListener(new TestRepositoryChangeListener());
    testConfigRepository.sync();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasKind(SpanKind.INTERNAL)
                        .hasName("Apollo Config Repository Change")
                        .hasAttributesSatisfyingExactly(
                            singletonList(
                                equalTo(AttributeKey.stringKey("config.namespace"), namespace)))));
  }

  static class TestConfigRepository extends AbstractConfigRepository {

    final String namespace;

    public TestConfigRepository(String namespace) {
      this.namespace = namespace;
    }

    @Override
    protected void sync() {
      this.fireRepositoryChange(this.namespace, new Properties());
    }

    @Override
    public Properties getConfig() {
      return new Properties();
    }

    @Override
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {}

    @Override
    public ConfigSourceType getSourceType() {
      return ConfigSourceType.NONE;
    }
  }

  static class TestRepositoryChangeListener implements RepositoryChangeListener {

    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
      new AbstractConfigRepository() {
        @Override
        public Properties getConfig() {
          return newProperties;
        }

        @Override
        public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {}

        @Override
        public ConfigSourceType getSourceType() {
          return ConfigSourceType.NONE;
        }

        @Override
        protected void sync() {
          this.fireRepositoryChange(namespace, new Properties());
        }
      }.sync();
    }
  }
}
