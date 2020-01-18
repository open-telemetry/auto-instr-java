package io.opentelemetry.auto.instrumentation.grpc.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.opentelemetry.auto.api.MoreTags;
import io.opentelemetry.auto.instrumentation.api.SpanScopePair;
import io.opentelemetry.trace.Span;

import static io.opentelemetry.auto.instrumentation.grpc.client.GrpcClientDecorator.DECORATE;
import static io.opentelemetry.auto.instrumentation.grpc.client.GrpcClientDecorator.TRACER;
import static io.opentelemetry.auto.instrumentation.grpc.client.GrpcInjectAdapter.SETTER;

public class TracingClientInterceptor implements ClientInterceptor {

  public static final TracingClientInterceptor INSTANCE = new TracingClientInterceptor();

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      final MethodDescriptor<ReqT, RespT> method,
      final CallOptions callOptions,
      final Channel next) {

    final Span span = TRACER.spanBuilder("grpc.client").startSpan();
    span.setAttribute(MoreTags.RESOURCE_NAME, method.getFullMethodName());
    final SpanScopePair spanAndScope = new SpanScopePair(span, TRACER.withSpan(span));
    try {
      DECORATE.afterStart(span);

      final ClientCall<ReqT, RespT> result;
      try {
        // call other interceptors
        result = next.newCall(method, callOptions);
      } catch (final Throwable e) {
        DECORATE.onError(span, e);
        DECORATE.beforeFinish(span);
        span.end();
        throw e;
      }
      return new TracingClientCall<>(span, result);
    } finally {
      spanAndScope.getScope().close();
    }
  }

  static final class TracingClientCall<ReqT, RespT>
      extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {
    final Span span;

    TracingClientCall(final Span span, final ClientCall<ReqT, RespT> delegate) {
      super(delegate);
      this.span = span;
    }

    @Override
    public void start(final Listener<RespT> responseListener, final Metadata headers) {
      TRACER.getHttpTextFormat().inject(span.getContext(), headers, SETTER);
      final SpanScopePair spanAndScope = new SpanScopePair(span, TRACER.withSpan(span));
      try {
        super.start(new TracingClientCallListener<>(span, responseListener), headers);
      } catch (final Throwable e) {
        DECORATE.onError(span, e);
        DECORATE.beforeFinish(span);
        span.end();
        throw e;
      } finally {
        spanAndScope.getScope().close();
      }
    }

    @Override
    public void sendMessage(final ReqT message) {
      final SpanScopePair spanAndScope = new SpanScopePair(span, TRACER.withSpan(span));
      try {
        super.sendMessage(message);
      } catch (final Throwable e) {
        DECORATE.onError(span, e);
        DECORATE.beforeFinish(span);
        span.end();
        throw e;
      } finally {
        spanAndScope.getScope().close();
      }
    }
  }

  static final class TracingClientCallListener<RespT>
      extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
    final Span span;

    TracingClientCallListener(final Span span, final ClientCall.Listener<RespT> delegate) {
      super(delegate);
      this.span = span;
    }

    @Override
    public void onMessage(final RespT message) {
      System.out.println("onMessage");
      final Span messageSpan = TRACER.spanBuilder("grpc.message").setParent(span).startSpan();
      messageSpan.setAttribute("message.type", message.getClass().getName());
      DECORATE.afterStart(messageSpan);
      final SpanScopePair spanAndScope = new SpanScopePair(span, TRACER.withSpan(span));
      try {
        delegate().onMessage(message);
      } catch (final Throwable e) {
        DECORATE.onError(messageSpan, e);
        throw e;
      } finally {
        DECORATE.beforeFinish(messageSpan);
        messageSpan.end();
        spanAndScope.getScope().close();
      }
    }

    @Override
    public void onClose(final Status status, final Metadata trailers) {
      DECORATE.onClose(span, status);
      // Finishes span.
      final SpanScopePair spanAndScope = new SpanScopePair(span, TRACER.withSpan(span));
      try {
        delegate().onClose(status, trailers);
      } catch (final Throwable e) {
        DECORATE.onError(span, e);
        throw e;
      } finally {
        DECORATE.beforeFinish(span);
        span.end();
        spanAndScope.getScope().close();
      }
    }

    @Override
    public void onReady() {
      final SpanScopePair spanAndScope = new SpanScopePair(span, TRACER.withSpan(span));
      try {
        delegate().onReady();
      } catch (final Throwable e) {
        DECORATE.onError(span, e);
        DECORATE.beforeFinish(span);
        span.end();
        throw e;
      } finally {
        spanAndScope.getScope().close();
      }
    }
  }
}
