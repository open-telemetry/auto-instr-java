/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import static io.opentelemetry.instrumentation.rabbitmq.RabbitTelemetry.RABBITMQ_COMMAND;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.semconv.SemanticAttributes;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InstrumentedChannel {

  private final Channel delegate;
  private final String exchange;
  private final String routingKey;
  private Map<String, Object> headers;

  private final RabbitTelemetry rabbitTelemetry;

  public InstrumentedChannel(
      RabbitTelemetry rabbitTelemetry, Channel delegate, String exchange, String routingKey) {
    this.rabbitTelemetry = rabbitTelemetry;
    this.delegate = delegate;
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.headers = new HashMap<>();
  }

  public Channel getChannel() {
    return delegate;
  }

  public void setHeaders(Map<String, Object> headers) {
    this.headers = headers;
  }

  public static String spanName(InstrumentedChannel channel) {
    return "producer";
  }

  public void publish(byte[] body) throws IOException {
    publish(body, new AMQP.BasicProperties().builder().build());
  }

  public void publish(byte[] body, AMQP.BasicProperties properties) throws IOException {
    Context context = rabbitTelemetry.getChannelInstrumenter().start(Context.current(), this);
    Throwable throwable = null;

    Span span = Span.fromContext(context);
    try (Scope scope = context.makeCurrent()) {
      onPublish(span);
      if (body != null) {
        span.setAttribute(
            SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, (long) body.length);
      }
      onProps(span, properties);
      this.publish(context, body, properties);
      this.setHeaders(properties.getHeaders());
      setInstrumentedChannel(context, this);
    } catch (Throwable exception) {
      throwable = exception;
    } finally {
      rabbitTelemetry.getChannelInstrumenter().end(context, this, null, throwable);
    }
  }

  private void setInstrumentedChannel(Context context, InstrumentedChannel instrumentedChannel) {
    MessageHeadersHolder holder = context.get(RabbitTelemetry.MESSAGE_HEADERS_CONTEXT_KEY);
    if (holder != null) {
      holder.setInstrumentedChannel(instrumentedChannel);
    }
  }

  private void onPublish(Span span) {
    String exchangeName = normalizeExchangeName(exchange);
    span.setAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, exchangeName);
    span.updateName(exchangeName + " publish");
    if (routingKey != null && !routingKey.isEmpty()) {
      span.setAttribute(SemanticAttributes.MESSAGING_RABBITMQ_DESTINATION_ROUTING_KEY, routingKey);
    }
    span.setAttribute(RABBITMQ_COMMAND, "basic.publish");
  }

  private void onProps(Span span, AMQP.BasicProperties props) {
    Integer deliveryMode = props.getDeliveryMode();
    if (deliveryMode != null) {
      span.setAttribute("rabbitmq.delivery_mode", deliveryMode);
    }
  }

  private static String normalizeExchangeName(String exchange) {
    return exchange == null || exchange.isEmpty() ? "<default>" : exchange;
  }

  private void publish(Context context, byte[] body, AMQP.BasicProperties properties)
      throws IOException {

    Map<String, Object> headers = properties.getHeaders();
    headers = (headers == null) ? new HashMap<>() : new HashMap<>(headers);

    GlobalOpenTelemetry.getPropagators()
        .getTextMapPropagator()
        .inject(context, headers, MapSetter.INSTANCE);

    AMQP.BasicProperties props =
        new AMQP.BasicProperties(
            properties.getContentType(),
            properties.getContentEncoding(),
            headers,
            properties.getDeliveryMode(),
            properties.getPriority(),
            properties.getCorrelationId(),
            properties.getReplyTo(),
            properties.getExpiration(),
            properties.getMessageId(),
            properties.getTimestamp(),
            properties.getType(),
            properties.getUserId(),
            properties.getAppId(),
            properties.getClusterId());

    delegate.basicPublish(exchange, routingKey, props, body);
  }

  public GetResponse basicGet(String queue, boolean autoAck) throws IOException {
    Timer timer = Timer.start();
    Context parentContext = Context.current();

    GetResponse response = null;
    Throwable throwable = null;
    try {
      response = delegate.basicGet(queue, autoAck);
    } catch (IOException exception) {
      throwable = exception;
    }

    ReceiveRequest request = new ReceiveRequest(queue, response, getChannel().getConnection());
    if (!rabbitTelemetry.getReceiveInstrumenter().shouldStart(parentContext, request)) {
      return null;
    }
    // can't create span and put into scope in method enter above, because can't add parent after
    // span creation
    InstrumenterUtil.startAndEnd(
        rabbitTelemetry.getReceiveInstrumenter(),
        parentContext,
        request,
        null,
        throwable,
        timer.startTime(),
        timer.now());

    return response;
  }

  public Map<String, Object> getHeaders() {
    return headers;
  }
}
