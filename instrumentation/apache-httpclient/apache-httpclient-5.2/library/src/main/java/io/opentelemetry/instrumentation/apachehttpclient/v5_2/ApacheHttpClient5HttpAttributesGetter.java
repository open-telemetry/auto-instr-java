/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachehttpclient.v5_2;

import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.MessageHeaders;
import org.apache.hc.core5.http.ProtocolVersion;

@Deprecated
enum ApacheHttpClient5HttpAttributesGetter
    implements HttpClientAttributesGetter<ApacheHttpClient5Request, HttpResponse> {
  INSTANCE;

  @Override
  public String getHttpRequestMethod(ApacheHttpClient5Request request) {
    return request.getMethod();
  }

  @Override
  @Nullable
  public String getUrlFull(ApacheHttpClient5Request request) {
    return request.getUrl();
  }

  @Override
  public List<String> getHttpRequestHeader(ApacheHttpClient5Request request, String name) {
    return getHeader(request, name);
  }

  @Override
  public Integer getHttpResponseStatusCode(
      ApacheHttpClient5Request request, HttpResponse response, @Nullable Throwable error) {
    return response.getCode();
  }

  @Override
  public List<String> getHttpResponseHeader(
      ApacheHttpClient5Request request, HttpResponse response, String name) {
    return getHeader(response, name);
  }

  private static List<String> getHeader(MessageHeaders messageHeaders, String name) {
    return headersToList(messageHeaders.getHeaders(name));
  }

  private static List<String> getHeader(ApacheHttpClient5Request messageHeaders, String name) {
    return headersToList(messageHeaders.getDelegate().getHeaders(name));
  }

  // minimize memory overhead by not using streams
  private static List<String> headersToList(Header[] headers) {
    if (headers.length == 0) {
      return Collections.emptyList();
    }
    List<String> headersList = new ArrayList<>(headers.length);
    for (Header header : headers) {
      headersList.add(header.getValue());
    }
    return headersList;
  }

  @Nullable
  @Override
  public String getNetworkProtocolName(
      ApacheHttpClient5Request request, @Nullable HttpResponse response) {
    ProtocolVersion protocolVersion = getVersion(request, response);
    if (protocolVersion == null) {
      return null;
    }
    return protocolVersion.getProtocol();
  }

  @Nullable
  @Override
  public String getNetworkProtocolVersion(
      ApacheHttpClient5Request request, @Nullable HttpResponse response) {
    ProtocolVersion protocolVersion = getVersion(request, response);
    if (protocolVersion == null) {
      return null;
    }
    if (protocolVersion.getMinor() == 0) {
      return Integer.toString(protocolVersion.getMajor());
    }
    return protocolVersion.getMajor() + "." + protocolVersion.getMinor();
  }

  @Override
  @Nullable
  public String getServerAddress(ApacheHttpClient5Request request) {
    return request.getDelegate().getAuthority().getHostName();
  }

  @Override
  public Integer getServerPort(ApacheHttpClient5Request request) {
    return request.getDelegate().getAuthority().getPort();
  }

  private static ProtocolVersion getVersion(
      ApacheHttpClient5Request request, @Nullable HttpResponse response) {
    ProtocolVersion protocolVersion = request.getDelegate().getVersion();
    if (protocolVersion == null && response != null) {
      protocolVersion = response.getVersion();
    }
    return protocolVersion;
  }
}
