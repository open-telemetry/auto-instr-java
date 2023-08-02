/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TType;

public final class ThriftRequest {
  public TProtocol iport;
  private final Map<String, String> attachments = new HashMap<String, String>();

  public Map<String, String> args = new HashMap<String, String>();
  public String methodName;

  public String host;

  public int port;

  public ThriftRequest(TProtocol port) {
    this.iport = port;
    this.methodName = "Thrift Call";
    this.port = 100;
    this.host = "0.0.0.0";
  }

  @CanIgnoreReturnValue
  public ThriftRequest setAttachment(String key, String value) {
    if (value == null) {
      this.attachments.remove(key);
    } else {
      this.attachments.put(key, value);
    }
    return this;
  }

  public Map<String, String> getAttachments() {
    return this.attachments;
  }

  @Nullable
  public String getAttachment(String key) {
    return this.attachments.get(key);
  }

  public String getMethodName() {
    return this.methodName;
  }

  public void writeAttachment() throws TException {
    TField field =
        new TField("thriftHeader", TType.MAP, AbstractProtocolWrapper.OIJ_THRIFT_FIELD_ID);
    iport.writeFieldBegin(field);
    try {
      Map<String, String> traceInfo = this.attachments;
      iport.writeMapBegin(new TMap(TType.STRING, TType.STRING, traceInfo.size()));
      for (Map.Entry<String, String> entry : traceInfo.entrySet()) {
        iport.writeString(entry.getKey());
        iport.writeString(entry.getValue());
      }
      iport.writeMapEnd();
    } finally {
      iport.writeFieldEnd();
    }
  }

  public void addArgs(String key, String value) {
    this.args.put(key, value);
  }
}
