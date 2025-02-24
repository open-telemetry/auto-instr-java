/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import com.amazonaws.Response;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

enum SqsProcessRequestAttributesGetter
    implements
        MessagingAttributesGetter<SqsProcessRequest, Response<?>>,
        MessagingNetworkAttributesGetter<SqsProcessRequest, Response<?>> {
  INSTANCE;

  // copied from MessagingIncubatingAttributes.MessagingSystemIncubatingValues
  private static final String AWS_SQS = "aws_sqs";

  @Override
  public String getSystem(SqsProcessRequest request) {
    return AWS_SQS;
  }

  @Override
  public String getDestination(SqsProcessRequest request) {
    Object originalRequest = request.getRequest().getOriginalRequest();
    String queueUrl = RequestAccess.getQueueUrl(originalRequest);
    int i = queueUrl.lastIndexOf('/');
    return i > 0 ? queueUrl.substring(i + 1) : null;
  }

  @Nullable
  @Override
  public String getDestinationTemplate(SqsProcessRequest request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(SqsProcessRequest request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(SqsProcessRequest request) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(SqsProcessRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(SqsProcessRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(SqsProcessRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId(SqsProcessRequest request, @Nullable Response<?> response) {
    return request.getMessage().getMessageId();
  }

  @Nullable
  @Override
  public String getClientId(SqsProcessRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(SqsProcessRequest request, @Nullable Response<?> response) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(SqsProcessRequest request, String name) {
    String value = request.getMessage().getMessageAttribute(name);
    return value != null ? Collections.singletonList(value) : Collections.emptyList();
  }
}
