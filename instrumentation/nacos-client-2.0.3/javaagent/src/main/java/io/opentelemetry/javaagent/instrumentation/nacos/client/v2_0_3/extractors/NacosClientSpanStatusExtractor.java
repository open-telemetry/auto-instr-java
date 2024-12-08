package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_3.extractors;

import com.alibaba.nacos.api.remote.response.Response;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_3.NacosClientRequest;
import javax.annotation.Nullable;

public class NacosClientSpanStatusExtractor implements
    SpanStatusExtractor<NacosClientRequest, Response> {
  @Override
  public void extract(SpanStatusBuilder spanStatusBuilder, NacosClientRequest nacosClientRequest,
      @Nullable Response response, @Nullable Throwable error) {
    if (response == null || !response.isSuccess()) {
      spanStatusBuilder.setStatus(StatusCode.ERROR);
    } else {
      SpanStatusExtractor.getDefault()
          .extract(spanStatusBuilder, nacosClientRequest, response, error);
    }
  }
}
