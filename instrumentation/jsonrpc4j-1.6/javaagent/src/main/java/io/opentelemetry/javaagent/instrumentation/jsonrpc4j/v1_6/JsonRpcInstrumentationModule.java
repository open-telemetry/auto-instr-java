package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_6;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class JsonRpcInstrumentationModule extends InstrumentationModule {
  public JsonRpcInstrumentationModule() {
    super("jsonrpc4j", "jsonrpc4j-1.6");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new JsonRpcServerBuilderInstrumentation(),
        new JsonServiceExporterBuilderInstrumentation(),
        new JsonRpcClientBuilderInstrumentation()
    );
  }
}
