/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

enum SqsAttributesGetter
    implements
        MessagingAttributesGetter<ExecutionAttributes, Response>,
        MessagingNetworkAttributesGetter<ExecutionAttributes, Response> {
  INSTANCE;

  // copied from MessagingIncubatingAttributes.MessagingSystemIncubatingValues
  private static final String AWS_SQS = "aws_sqs";

  @Override
  public String getSystem(ExecutionAttributes request) {
    return AWS_SQS;
  }

  @Override
  public String getDestination(ExecutionAttributes request) {
    SdkRequest sdkRequest = request.getAttribute(TracingExecutionInterceptor.SDK_REQUEST_ATTRIBUTE);
    String queueUrl = SqsAccess.getQueueUrl(sdkRequest);
    if (queueUrl != null) {
      int i = queueUrl.lastIndexOf('/');
      if (i > 0) {
        return queueUrl.substring(i + 1);
      }
    }
    return null;
  }

  @Nullable
  @Override
  public String getDestinationTemplate(ExecutionAttributes request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(ExecutionAttributes request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(ExecutionAttributes request) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(ExecutionAttributes request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(ExecutionAttributes request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(ExecutionAttributes request) {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId(ExecutionAttributes request, @Nullable Response response) {
    if (response != null && response.getSdkResponse() != null) {
      SdkResponse sdkResponse = response.getSdkResponse();
      return SqsAccess.getMessageId(sdkResponse);
    }
    return null;
  }

  @Nullable
  @Override
  public String getClientId(ExecutionAttributes request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(ExecutionAttributes request, @Nullable Response response) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(ExecutionAttributes request, String name) {
    SdkRequest sdkRequest = request.getAttribute(TracingExecutionInterceptor.SDK_REQUEST_ATTRIBUTE);
    String value = SqsAccess.getMessageAttribute(sdkRequest, name);
    return value != null ? Collections.singletonList(value) : Collections.emptyList();
  }
}
