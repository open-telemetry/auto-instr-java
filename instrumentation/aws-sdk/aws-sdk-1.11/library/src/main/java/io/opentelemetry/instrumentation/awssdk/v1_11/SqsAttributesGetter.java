/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

enum SqsAttributesGetter
    implements
        MessagingAttributesGetter<Request<?>, Response<?>>,
        MessagingNetworkAttributesGetter<Request<?>, Response<?>> {
  INSTANCE;

  // copied from MessagingIncubatingAttributes.MessagingSystemIncubatingValues
  private static final String AWS_SQS = "aws_sqs";

  @Override
  public String getSystem(Request<?> request) {
    return AWS_SQS;
  }

  @Override
  public String getDestination(Request<?> request) {
    Object originalRequest = request.getOriginalRequest();
    String queueUrl = RequestAccess.getQueueUrl(originalRequest);
    int i = queueUrl.lastIndexOf('/');
    return i > 0 ? queueUrl.substring(i + 1) : null;
  }

  @Nullable
  @Override
  public String getDestinationTemplate(Request<?> request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(Request<?> request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(Request<?> request) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(Request<?> request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(Request<?> request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(Request<?> request) {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId(Request<?> request, @Nullable Response<?> response) {
    return SqsAccess.getMessageId(response);
  }

  @Nullable
  @Override
  public String getClientId(Request<?> request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(Request<?> request, @Nullable Response<?> response) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(Request<?> request, String name) {
    String value = SqsAccess.getMessageAttribute(request, name);
    return value != null ? Collections.singletonList(value) : Collections.emptyList();
  }
}
