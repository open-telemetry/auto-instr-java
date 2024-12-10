/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.armeria.v1_3;

import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.server.HttpService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.config.internal.CommonConfig;
import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaClientTelemetry;
import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaClientTelemetryBuilder;
import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaServerTelemetry;
import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaServerTelemetryBuilder;
import io.opentelemetry.instrumentation.armeria.v1_3.internal.ArmeriaInstrumenterBuilderUtil;
import io.opentelemetry.javaagent.bootstrap.internal.AgentCommonConfig;
import java.util.function.Function;

// Holds singleton references to decorators to match against during suppression.
// https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/903
public final class ArmeriaSingletons {
  public static final Function<? super HttpClient, ? extends HttpClient> CLIENT_DECORATOR;

  public static final Function<? super HttpService, ? extends HttpService> SERVER_DECORATOR;

  static {
    CommonConfig config = AgentCommonConfig.get();

    ArmeriaClientTelemetryBuilder clientBuilder =
        ArmeriaClientTelemetry.builder(GlobalOpenTelemetry.get());
    ArmeriaInstrumenterBuilderUtil.getClientBuilderExtractor()
        .apply(clientBuilder)
        .configure(config);
    ArmeriaClientTelemetry clientTelemetry = clientBuilder.build();

    ArmeriaServerTelemetryBuilder serverBuilder =
        ArmeriaServerTelemetry.builder(GlobalOpenTelemetry.get());
    ArmeriaInstrumenterBuilderUtil.getServerBuilderExtractor()
        .apply(serverBuilder)
        .configure(config);
    ArmeriaServerTelemetry serverTelemetry = serverBuilder.build();

    CLIENT_DECORATOR = clientTelemetry.newDecorator();
    Function<? super HttpService, ? extends HttpService> libraryDecorator =
        serverTelemetry.newDecorator().compose(ResponseCustomizingDecorator::new);
    SERVER_DECORATOR = service -> new ServerDecorator(service, libraryDecorator.apply(service));
  }

  private ArmeriaSingletons() {}
}
