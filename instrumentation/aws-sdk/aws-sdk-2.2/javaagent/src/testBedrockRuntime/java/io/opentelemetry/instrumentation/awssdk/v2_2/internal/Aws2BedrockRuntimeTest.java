/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import io.opentelemetry.instrumentation.awssdk.v2_2.AbstractAws2BedrockRuntimeTest;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

class Aws2BedrockRuntimeTest extends AbstractAws2BedrockRuntimeTest {
  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Override
  protected InstrumentationExtension getTesting() {
    return testing;
  }

  @Override
  protected ClientOverrideConfiguration.Builder createOverrideConfigurationBuilder() {
    return ClientOverrideConfiguration.builder();
  }
}
