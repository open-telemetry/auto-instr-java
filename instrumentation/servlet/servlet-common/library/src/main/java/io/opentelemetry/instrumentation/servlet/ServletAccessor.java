/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.servlet;

import java.security.Principal;

/**
 * This interface is used to access methods of ServletContext, HttpServletRequest and
 * HttpServletResponse classes in shared code that is used for both jakarta.servlet and
 * javax.servlet versions of those classes. A wrapper class with extra information attached may be
 * used as well in cases where the class itself does not provide some field (such as response status
 * for Servlet API 2.2).
 *
 * @param <SERVLETCONTEXT> ServletContext class (or a wrapper)
 * @param <REQUEST> HttpServletRequest class (or a wrapper)
 * @param <RESPONSE> HttpServletResponse class (or a wrapper)
 */
public interface ServletAccessor<SERVLETCONTEXT, REQUEST, RESPONSE> {
  Object getServletContextAttribute(SERVLETCONTEXT servletContext, String name);

  void setServletContextAttribute(SERVLETCONTEXT servletContext, String name, Object object);

  String getRequestContextPath(REQUEST request);

  String getRequestScheme(REQUEST request);

  String getRequestServerName(REQUEST request);

  int getRequestServerPort(REQUEST request);

  String getRequestUri(REQUEST request);

  String getRequestQueryString(REQUEST request);

  Object getRequestAttribute(REQUEST request, String name);

  void setRequestAttribute(REQUEST request, String name, Object value);

  String getRequestProtocol(REQUEST request);

  String getRequestMethod(REQUEST request);

  String getRequestRemoteAddr(REQUEST request);

  String getRequestHeader(REQUEST request, String name);

  String getRequestServletPath(REQUEST request);

  String getRequestPathInfo(REQUEST request);

  Principal getRequestUserPrincipal(REQUEST request);

  Integer getRequestRemotePort(REQUEST request);

  boolean isRequestAsyncStarted(REQUEST request);

  void addRequestAsyncListener(REQUEST request, ServletAsyncListener<RESPONSE> listener);

  int getResponseStatus(RESPONSE response);

  boolean isResponseCommitted(RESPONSE response);

  boolean isServletException(Throwable throwable);
}
