package io.opentelemetry.instrumentation.awssdk.v2_2;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.javaagent.tooling.muzzle.NoMuzzle;
import software.amazon.awssdk.core.SdkRequest;

// helper class for calling methods that use sqs types in SqsImpl
// if SqsImpl is not present these methods are no op
public final class SqsAccess {
  private static final boolean enabled = isSqsImplPresent();

  private static boolean isSqsImplPresent() {
    try {
      // for library instrumentation SqsImpl is always available
      // for javaagent instrumentation SqsImpl is available only when SqsInstrumentationModule was
      // successfully applied (muzzle passed)
      // using package name here because library instrumentation classes are relocated when embedded
      // in the agent
      Class.forName(SqsAccess.class.getPackage().getName() + ".SqsImpl");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @NoMuzzle
  public static SdkRequest injectIntoSqsSendMessageRequest(TextMapPropagator messagingPropagator,
      SdkRequest rawRequest, io.opentelemetry.context.Context otelContext) {
    if (!enabled) {
      return rawRequest;
    }
    return SqsImpl.injectIntoSqsSendMessageRequest(messagingPropagator, rawRequest, otelContext);
  }

  private SqsAccess() {}
}
