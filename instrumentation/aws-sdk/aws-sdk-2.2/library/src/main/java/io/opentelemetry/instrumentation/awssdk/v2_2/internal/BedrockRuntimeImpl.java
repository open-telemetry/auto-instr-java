/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import java.util.List;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

final class BedrockRuntimeImpl {
  private BedrockRuntimeImpl() {}

  static boolean isBedrockRuntimeRequest(SdkRequest request) {
    if (request instanceof ConverseRequest) {
      return true;
    }
    return false;
  }

  @Nullable
  static String getModelId(SdkRequest request) {
    if (request instanceof ConverseRequest) {
      return ((ConverseRequest) request).modelId();
    }
    return null;
  }

  @Nullable
  static Long getMaxTokens(SdkRequest request) {
    if (request instanceof ConverseRequest) {
      InferenceConfiguration config = ((ConverseRequest) request).inferenceConfig();
      if (config != null) {
        return integerToLong(config.maxTokens());
      }
    }
    return null;
  }

  @Nullable
  static Double getTemperature(SdkRequest request) {
    if (request instanceof ConverseRequest) {
      InferenceConfiguration config = ((ConverseRequest) request).inferenceConfig();
      if (config != null) {
        return floatToDouble(config.temperature());
      }
    }
    return null;
  }

  @Nullable
  static Double getTopP(SdkRequest request) {
    if (request instanceof ConverseRequest) {
      InferenceConfiguration config = ((ConverseRequest) request).inferenceConfig();
      if (config != null) {
        return floatToDouble(config.topP());
      }
    }
    return null;
  }

  @Nullable
  static List<String> getStopSequences(SdkRequest request) {
    if (request instanceof ConverseRequest) {
      InferenceConfiguration config = ((ConverseRequest) request).inferenceConfig();
      if (config != null) {
        return config.stopSequences();
      }
    }
    return null;
  }

  @Nullable
  static String getStopReason(SdkResponse response) {
    if (response instanceof ConverseResponse) {
      StopReason reason = ((ConverseResponse) response).stopReason();
      if (reason != null) {
        return reason.toString();
      }
    }
    return null;
  }

  @Nullable
  static Long getUsageInputTokens(SdkResponse response) {
    if (response instanceof ConverseResponse) {
      TokenUsage usage = ((ConverseResponse) response).usage();
      if (usage != null) {
        return integerToLong(usage.inputTokens());
      }
    }
    return null;
  }

  @Nullable
  static Long getUsageOutputTokens(SdkResponse response) {
    if (response instanceof ConverseResponse) {
      TokenUsage usage = ((ConverseResponse) response).usage();
      if (usage != null) {
        return integerToLong(usage.outputTokens());
      }
    }
    return null;
  }

  @Nullable
  private static Long integerToLong(Integer value) {
    if (value == null) {
      return null;
    }
    return Long.valueOf(value);
  }

  @Nullable
  private static Double floatToDouble(Float value) {
    if (value == null) {
      return null;
    }
    return Double.valueOf(value);
  }
}
