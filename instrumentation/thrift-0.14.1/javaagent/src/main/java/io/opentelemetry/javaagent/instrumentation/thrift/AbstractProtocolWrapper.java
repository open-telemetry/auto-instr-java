/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolDecorator;

public abstract class AbstractProtocolWrapper extends TProtocolDecorator {
  public static final String OIJ_THRIFT_FIELD_NAME = "OIJ_FIELD_NAME";
  public static final short OIJ_THRIFT_FIELD_ID = 3333;

  public AbstractProtocolWrapper(TProtocol protocol) {
    super(protocol);
  }
}
