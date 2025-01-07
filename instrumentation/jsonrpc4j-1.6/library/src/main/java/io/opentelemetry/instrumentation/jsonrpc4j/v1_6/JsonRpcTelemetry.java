package io.opentelemetry.instrumentation.jsonrpc4j.v1_6;


import com.googlecode.jsonrpc4j.InvocationListener;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

public final class JsonRpcTelemetry {
  public static JsonRpcTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  public static JsonRpcTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new JsonRpcTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<JsonRpcRequest, JsonRpcResponse> serverInstrumenter;
  private final Instrumenter<SimpleJsonRpcRequest, SimpleJsonRpcResponse> clientInstrumenter;

  JsonRpcTelemetry(
      Instrumenter<JsonRpcRequest, JsonRpcResponse> serverInstrumenter,
      Instrumenter<SimpleJsonRpcRequest, SimpleJsonRpcResponse> clientInstrumenter,
      ContextPropagators propagators) {
    this.serverInstrumenter = serverInstrumenter;
    this.clientInstrumenter = clientInstrumenter;
  }


  public InvocationListener newServerInvocationListener() {
    return new OpenTelemetryJsonRpcInvocationListener(serverInstrumenter);
  }

  public Instrumenter<SimpleJsonRpcRequest, SimpleJsonRpcResponse> getClientInstrumenter() {
    return clientInstrumenter;
  }
}
