/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_TEST_ARGUMENTS;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_TEST_LIFECYCLE;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_TEST_RESULT;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;

class JunitAttributesExtractor implements AttributesExtractor<ExtensionContext, Object> {

  private static final AttributeKey<String> TEST_RESULT = AttributeKey.stringKey(JUNIT_TEST_RESULT);
  private static final AttributeKey<String> TEST_LIFECYCLE =
      AttributeKey.stringKey(JUNIT_TEST_LIFECYCLE);
  private static final AttributeKey<String> TEST_TAG = AttributeKey.stringKey("junit.test.tag");
  private static final AttributeKey<String> TEST_CLASS = AttributeKey.stringKey("junit.test.class");
  private static final AttributeKey<String> TEST_METHOD =
      AttributeKey.stringKey("junit.test.method");
  private static final AttributeKey<List<String>> TEST_ARGUMENTS =
      AttributeKey.stringArrayKey(JUNIT_TEST_ARGUMENTS);

  public static AttributesExtractor<ExtensionContext, Object> create() {
    return new JunitAttributesExtractor();
  }

  private JunitAttributesExtractor() {}

  @SuppressWarnings("unchecked")
  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, ExtensionContext context) {

    context.getTags().forEach(tag -> internalSet(attributes, TEST_TAG, tag));
    context
        .getTestClass()
        .ifPresent(testClass -> internalSet(attributes, TEST_CLASS, testClass.getCanonicalName()));
    context
        .getTestMethod()
        .ifPresent(testMethod -> internalSet(attributes, TEST_METHOD, testMethod.getName()));
    Optional.ofNullable(
            context
                .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
                .get(JUNIT_TEST_LIFECYCLE, String.class))
        .ifPresent(lifecycle -> internalSet(attributes, TEST_LIFECYCLE, lifecycle));
    Optional.ofNullable(
            context
                .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
                .get(JUNIT_TEST_ARGUMENTS, List.class))
        .ifPresent(arguments -> internalSet(attributes, TEST_ARGUMENTS, arguments));
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      ExtensionContext extensionContext,
      @Nullable Object response,
      @Nullable Throwable error) {
    Optional.ofNullable(
            extensionContext
                .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
                .get(JUNIT_TEST_RESULT, String.class))
        .ifPresent(testResult -> internalSet(attributes, TEST_RESULT, testResult));
  }
}
