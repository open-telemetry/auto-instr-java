/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.TracingExecutionInterceptor.SDK_REQUEST_ATTRIBUTE;

import io.opentelemetry.instrumentation.api.incubator.semconv.genai.GenAiAttributesGetter;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

enum BedrockRuntimeAttributesGetter
    implements GenAiAttributesGetter<ExecutionAttributes, Response> {
  INSTANCE;

  // copied from GenAiIncubatingAttributes
  private static final class GenAiOperationNameIncubatingValues {
    static final String CHAT = "chat";

    private GenAiOperationNameIncubatingValues() {}
  }

  private static final class GenAiSystemIncubatingValues {
    static final String AWS_BEDROCK = "aws.bedrock";

    private GenAiSystemIncubatingValues() {}
  }

  @Override
  public String getOperationName(ExecutionAttributes executionAttributes) {
    String operation = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
    if (operation != null) {
      switch (operation) {
        case "Converse":
          return GenAiOperationNameIncubatingValues.CHAT;
        default:
          return null;
      }
    }
    return null;
  }

  @Override
  public String getSystem(ExecutionAttributes executionAttributes) {
    return GenAiSystemIncubatingValues.AWS_BEDROCK;
  }

  @Nullable
  @Override
  public String getRequestModel(ExecutionAttributes executionAttributes) {
    return BedrockRuntimeAccess.getModelId(executionAttributes.getAttribute(SDK_REQUEST_ATTRIBUTE));
  }

  @Nullable
  @Override
  public Long getRequestSeed(ExecutionAttributes executionAttributes) {
    return null;
  }

  @Nullable
  @Override
  public List<String> getRequestEncodingFormats(ExecutionAttributes executionAttributes) {
    return null;
  }

  @Nullable
  @Override
  public Double getRequestFrequencyPenalty(ExecutionAttributes executionAttributes) {
    return null;
  }

  @Nullable
  @Override
  public Long getRequestMaxTokens(ExecutionAttributes executionAttributes) {
    return BedrockRuntimeAccess.getMaxTokens(
        executionAttributes.getAttribute(SDK_REQUEST_ATTRIBUTE));
  }

  @Nullable
  @Override
  public Double getRequestPresencePenalty(ExecutionAttributes executionAttributes) {
    return null;
  }

  @Nullable
  @Override
  public List<String> getRequestStopSequences(ExecutionAttributes executionAttributes) {
    return BedrockRuntimeAccess.getStopSequences(
        executionAttributes.getAttribute(SDK_REQUEST_ATTRIBUTE));
  }

  @Nullable
  @Override
  public Double getRequestTemperature(ExecutionAttributes executionAttributes) {
    return BedrockRuntimeAccess.getTemperature(
        executionAttributes.getAttribute(SDK_REQUEST_ATTRIBUTE));
  }

  @Nullable
  @Override
  public Double getRequestTopK(ExecutionAttributes executionAttributes) {
    return null;
  }

  @Nullable
  @Override
  public Double getRequestTopP(ExecutionAttributes executionAttributes) {
    return BedrockRuntimeAccess.getTopP(executionAttributes.getAttribute(SDK_REQUEST_ATTRIBUTE));
  }

  @Nullable
  @Override
  public List<String> getResponseFinishReasons(
      ExecutionAttributes executionAttributes, Response response) {
    String stopReason = BedrockRuntimeAccess.getStopReason(response.getSdkResponse());
    if (stopReason == null) {
      return null;
    }
    return Arrays.asList(stopReason);
  }

  @Nullable
  @Override
  public String getResponseId(ExecutionAttributes executionAttributes, Response response) {
    return null;
  }

  @Nullable
  @Override
  public String getResponseModel(ExecutionAttributes executionAttributes, Response response) {
    return null;
  }

  @Nullable
  @Override
  public Long getUsageInputTokens(ExecutionAttributes executionAttributes, Response response) {
    return BedrockRuntimeAccess.getUsageInputTokens(response.getSdkResponse());
  }

  @Nullable
  @Override
  public Long getUsageOutputTokens(ExecutionAttributes executionAttributes, Response response) {
    return BedrockRuntimeAccess.getUsageOutputTokens(response.getSdkResponse());
  }
}
