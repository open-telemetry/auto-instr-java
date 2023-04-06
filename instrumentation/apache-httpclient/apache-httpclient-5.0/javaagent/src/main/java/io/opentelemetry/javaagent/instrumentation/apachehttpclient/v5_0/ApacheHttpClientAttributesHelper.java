/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.MessageHeaders;
import org.apache.hc.core5.http.ProtocolVersion;
import org.apache.hc.core5.net.URIAuthority;

public final class ApacheHttpClientAttributesHelper {
  private static final Logger logger;

  static {
    logger = Logger.getLogger(ApacheHttpClientAttributesHelper.class.getName());
  }

  public ApacheHttpClientAttributesHelper() {}

  public static List<String> getHeader(MessageHeaders messageHeaders, String name) {
    return headersToList(messageHeaders.getHeaders(name));
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

  public static String getFlavor(ProtocolVersion protocolVersion) {
    if (protocolVersion == null) {
      return null;
    }
    String protocol = protocolVersion.getProtocol();
    if (!"HTTP".equals(protocol)) {
      return null;
    }
    int major = protocolVersion.getMajor();
    int minor = protocolVersion.getMinor();
    if (major == 1 && minor == 0) {
      return SemanticAttributes.HttpFlavorValues.HTTP_1_0;
    }
    if (major == 1 && minor == 1) {
      return SemanticAttributes.HttpFlavorValues.HTTP_1_1;
    }
    if (major == 2 && minor == 0) {
      return SemanticAttributes.HttpFlavorValues.HTTP_2_0;
    }
    logger.log(Level.FINE, "unexpected http protocol version: {0}", protocolVersion);
    return null;
  }

  public static String getUrl(HttpRequest httpRequest) {
    // similar to org.apache.hc.core5.http.message.BasicHttpRequest.getUri()
    // not calling getUri() to avoid unnecessary conversion
    StringBuilder url = new StringBuilder();
    URIAuthority authority = httpRequest.getAuthority();
    if (authority != null) {
      String scheme = httpRequest.getScheme();
      if (scheme != null) {
        url.append(scheme);
        url.append("://");
      } else {
        url.append("http://");
      }
      url.append(authority.getHostName());
      int port = authority.getPort();
      if (port >= 0) {
        url.append(":");
        url.append(port);
      }
    }
    String path = httpRequest.getPath();
    if (path != null) {
      if (url.length() > 0 && !path.startsWith("/")) {
        url.append("/");
      }
      url.append(path);
    } else {
      url.append("/");
    }
    return url.toString();
  }

  public static Integer getPeerPort(HttpRequest httpRequest) {
    URIAuthority uriAuthority = httpRequest.getAuthority();
    return uriAuthority != null ? uriAuthority.getPort() : null;
  }

  public static String getPeerName(HttpRequest httpRequest) {
    URIAuthority uriAuthority = httpRequest.getAuthority();
    return uriAuthority != null ? uriAuthority.getHostName() : null;
  }

  public static InetSocketAddress getPeerSocketAddress(HttpHost target) {
    if (target == null) {
      return null;
    }
    InetAddress inetAddress = target.getAddress();
    return inetAddress == null ? null : new InetSocketAddress(inetAddress, target.getPort());
  }
}
