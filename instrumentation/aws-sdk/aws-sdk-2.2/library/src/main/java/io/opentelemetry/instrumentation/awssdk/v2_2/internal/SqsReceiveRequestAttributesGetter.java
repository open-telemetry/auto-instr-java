/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.SdkRequest;

enum SqsReceiveRequestAttributesGetter
    implements
        MessagingAttributesGetter<SqsReceiveRequest, Response>,
        MessagingNetworkAttributesGetter<SqsReceiveRequest, Response> {
  INSTANCE;

  // copied from MessagingIncubatingAttributes.MessagingSystemIncubatingValues
  private static final String AWS_SQS = "aws_sqs";

  @Override
  public String getSystem(SqsReceiveRequest request) {
    return AWS_SQS;
  }

  @Override
  public String getDestination(SqsReceiveRequest request) {
    SdkRequest sdkRequest =
        request.getRequest().getAttribute(TracingExecutionInterceptor.SDK_REQUEST_ATTRIBUTE);
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
  public String getDestinationTemplate(SqsReceiveRequest request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(SqsReceiveRequest request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(SqsReceiveRequest request) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(SqsReceiveRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(SqsReceiveRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(SqsReceiveRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId(SqsReceiveRequest request, @Nullable Response response) {
    return null;
  }

  @Nullable
  @Override
  public String getClientId(SqsReceiveRequest request) {
    return null;
  }

  @Override
  public Long getBatchMessageCount(SqsReceiveRequest request, @Nullable Response response) {
    return (long) request.getMessages().size();
  }

  @Override
  public List<String> getMessageHeader(SqsReceiveRequest request, String name) {
    return StreamSupport.stream(request.getMessages().spliterator(), false)
        .map(message -> message.getMessageAttribute(name))
        .filter(value -> value != null)
        .collect(Collectors.toList());
  }
}
