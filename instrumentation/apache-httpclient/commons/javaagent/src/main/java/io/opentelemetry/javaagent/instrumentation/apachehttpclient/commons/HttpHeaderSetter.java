/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.context.propagation.TextMapSetter;
import javax.annotation.Nullable;

public final class HttpHeaderSetter implements TextMapSetter<OtelHttpRequest> {
  @Override
  public void set(@Nullable OtelHttpRequest carrier, String key, String value) {
    if (carrier != null) {
      carrier.setHeader(key, value);
    }
  }
}
