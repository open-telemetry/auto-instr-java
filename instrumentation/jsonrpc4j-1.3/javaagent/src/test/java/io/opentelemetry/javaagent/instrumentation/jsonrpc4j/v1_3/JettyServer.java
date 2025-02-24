/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import com.googlecode.jsonrpc4j.AnnotationsErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class JettyServer implements AutoCloseable {

  public static final String DEFAULT_LOCAL_HOSTNAME = "127.0.0.1";

  public static final String SERVLET = "someSunnyServlet";
  private static final String PROTOCOL = "http";

  private final Class<?> service;

  private Server jetty;
  private int port;

  JettyServer(Class<?> service) {
    this.service = service;
  }

  public String getCustomServerUrlString(String servletName) {
    return PROTOCOL + "://" + DEFAULT_LOCAL_HOSTNAME + ":" + port + "/" + servletName;
  }

  public void startup() throws Exception {
    jetty = new Server(0);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    context.setContextPath("/");
    jetty.setHandler(context);
    ServletHolder servlet = context.addServlet(JsonRpcTestServlet.class, "/" + SERVLET);
    servlet.setInitParameter("class", service.getCanonicalName());
    jetty.start();
    port = ((ServerConnector) jetty.getConnectors()[0]).getLocalPort();
  }

  @Override
  public void close() throws Exception {
    this.stop();
  }

  public void stop() throws Exception {
    jetty.stop();
  }

  public static class JsonRpcTestServlet extends HttpServlet {

    static final long serialVersionUID = 1L;
    private transient JsonRpcServer jsonRpcServer;

    @Override
    public void init() throws ServletException {
      try {
        Class<?> svcClass = Class.forName(getInitParameter("class"));
        Object instance = svcClass.getConstructor().newInstance();
        jsonRpcServer = new JsonRpcServer(instance);
        jsonRpcServer.setErrorResolver(AnnotationsErrorResolver.INSTANCE);
      } catch (ClassNotFoundException
          | NoSuchMethodException
          | InstantiationException
          | InvocationTargetException
          | IllegalAccessException e) {
        throw new ServletException(e);
      }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      jsonRpcServer.handle(request, response);
    }
  }
}
