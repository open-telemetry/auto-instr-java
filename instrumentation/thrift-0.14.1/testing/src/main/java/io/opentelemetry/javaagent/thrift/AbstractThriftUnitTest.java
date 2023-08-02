/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.thrift;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.assertj.core.api.Assertions.fail;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.javaagent.thrift.thrifttest.Account;
import io.opentelemetry.javaagent.thrift.thrifttest.HelloWorldService;
import io.opentelemetry.javaagent.thrift.thrifttest.User;
import io.opentelemetry.javaagent.thrift.thrifttest.UserAccount;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.IOException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractThriftUnitTest {
  public TServer server;

  protected abstract InstrumentationExtension testing();

  public int port = 9091;

  @BeforeEach
  public void before() {
    port += 1;
  }

  @AfterEach
  public void after() {
    stopServer();
  }

  @Test
  public void syncClientSimpleServer() throws TException {
    startSimpleServer();
    TTransport transport = new TSocket("localhost", port);
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);

    HelloWorldService.Client client = new HelloWorldService.Client(protocol);
    String response = testing().runWithSpan("parent", () -> client.sayHello("US", "Bob"));
    assertThat(response).isEqualTo("Hello " + "US" + "s' " + "Bob");
    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName("sayHello")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "sayHello")),
                    span ->
                        span.hasName("sayHello")
                            .hasKind(SpanKind.SERVER)
                            .hasParent(trace.getSpan(1))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "sayHello"))));
    transport.close();
  }

  @Test
  public void nonBlockClientNonBlockingServer() throws TException {
    startNonBlockingServer();
    TTransport transport = new TSocket("localhost", port);
    TFramedTransport framedTransport = new TFramedTransport(transport);
    framedTransport.open();
    TProtocol protocol = new TBinaryProtocol(framedTransport);
    HelloWorldService.Client client = new HelloWorldService.Client(protocol);

    String response = testing().runWithSpan("parent", () -> client.sayHello("US", "Bob"));
    assertThat(response).isEqualTo("Hello " + "US" + "s' " + "Bob");
    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName("sayHello")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "sayHello")),
                    span ->
                        span.hasName("sayHello")
                            .hasKind(SpanKind.SERVER)
                            .hasParent(trace.getSpan(1))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "sayHello"))));
    framedTransport.close();
  }

  @Test
  public void manyCallParallel() throws TException {
    startNonBlockingServer();
    for (int i = 0; i < 4; i++) {
      new Thread(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    TTransport transport = new TSocket("localhost", port);
                    TFramedTransport framedTransport = new TFramedTransport(transport);
                    framedTransport.open();
                    TProtocol protocol = new TBinaryProtocol(framedTransport);
                    HelloWorldService.Client client = new HelloWorldService.Client(protocol);

                    String response = testing().runWithSpan("parent", () -> client.withDelay(1));
                    assertThat(response).isEqualTo("delay " + 1);
                    testing()
                        .waitAndAssertTraces(
                            trace ->
                                trace.hasSpansSatisfyingExactly(
                                    span ->
                                        span.hasName("parent")
                                            .hasKind(SpanKind.INTERNAL)
                                            .hasNoParent(),
                                    span ->
                                        span.hasName("withDelay")
                                            .hasKind(SpanKind.CLIENT)
                                            .hasParent(trace.getSpan(0))
                                            .hasAttributesSatisfying(
                                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                                equalTo(
                                                    SemanticAttributes.RPC_SERVICE,
                                                    "thrift_Service"),
                                                equalTo(
                                                    SemanticAttributes.RPC_METHOD, "withDelay")),
                                    span ->
                                        span.hasName("withDelay")
                                            .hasKind(SpanKind.SERVER)
                                            .hasParent(trace.getSpan(1))
                                            .hasAttributesSatisfying(
                                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                                equalTo(
                                                    SemanticAttributes.RPC_SERVICE,
                                                    "thrift_Service"),
                                                equalTo(
                                                    SemanticAttributes.RPC_METHOD, "withDelay"))));
                  } catch (Exception e) {
                    fail(e.toString());
                  }
                }
              })
          .start();
    }
  }

  @Test
  public void asyncClientNonBlockingServer() throws TException, IOException {
    startNonBlockingServer();
    TAsyncClientManager clientManager = new TAsyncClientManager();
    TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
    HelloWorldService.AsyncClient.Factory factory =
        new HelloWorldService.AsyncClient.Factory(clientManager, protocolFactory);
    TNonblockingTransport nonblockingTransport = new TNonblockingSocket("localhost", port);
    HelloWorldService.AsyncClient asyClient = factory.getAsyncClient(nonblockingTransport);
    AsyncMethodCallback<String> callback =
        new AsyncMethodCallback<String>() {
          @Override
          public void onComplete(String s) {

            assertThat(s).isEqualTo("Hello " + "US" + "s' " + "Bob");
            testing()
                .waitAndAssertTraces(
                    trace ->
                        trace.hasSpansSatisfyingExactly(
                            span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                            span ->
                                span.hasName("sayHello")
                                    .hasKind(SpanKind.CLIENT)
                                    .hasParent(trace.getSpan(0))
                                    .hasAttributesSatisfying(
                                        equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                        equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                        equalTo(SemanticAttributes.RPC_METHOD, "sayHello")),
                            span ->
                                span.hasName("sayHello")
                                    .hasKind(SpanKind.SERVER)
                                    .hasParent(trace.getSpan(1))
                                    .hasAttributesSatisfying(
                                        equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                        equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                        equalTo(SemanticAttributes.RPC_METHOD, "sayHello"))));
          }

          @Override
          public void onError(Exception e) {
            fail(e.toString());
          }
        };
    testing().runWithSpan("parent", () -> asyClient.sayHello("US", "Bob", callback));
  }

  @Test
  public void asyncMany() throws TException, IOException {
    startNonBlockingServer();
    for (int i = 1; i < 4; i++) {
      TAsyncClientManager clientManager = new TAsyncClientManager();
      TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
      HelloWorldService.AsyncClient.Factory factory =
          new HelloWorldService.AsyncClient.Factory(clientManager, protocolFactory);
      TNonblockingTransport nonblockingTransport = new TNonblockingSocket("localhost", port);
      HelloWorldService.AsyncClient asyClient = factory.getAsyncClient(nonblockingTransport);
      String parentName = "parent" + i;
      AsyncMethodCallback<String> callback =
          new AsyncMethodCallback<String>() {
            @Override
            public void onComplete(String s) {
              assertThat(s).isEqualTo("delay " + 1);
              testing()
                  .waitAndAssertTraces(
                      trace ->
                          trace.hasSpansSatisfyingExactly(
                              span ->
                                  span.hasName(parentName).hasKind(SpanKind.INTERNAL).hasNoParent(),
                              span ->
                                  span.hasName("withDelay")
                                      .hasKind(SpanKind.CLIENT)
                                      .hasParent(trace.getSpan(0))
                                      .hasAttributesSatisfying(
                                          equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                          equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                          equalTo(SemanticAttributes.RPC_METHOD, "withDelay")),
                              span ->
                                  span.hasName("withDelay")
                                      .hasKind(SpanKind.SERVER)
                                      .hasParent(trace.getSpan(1))
                                      .hasAttributesSatisfying(
                                          equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                          equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                          equalTo(SemanticAttributes.RPC_METHOD, "withDelay"))));
            }

            @Override
            public void onError(Exception e) {
              fail(e.toString());
            }
          };
      testing().runWithSpan("parent" + i, () -> asyClient.withDelay(1, callback));
    }
  }

  @Test
  public void asyncClientAsyncServer() throws TException, IOException {
    startAsyncServer();
    TAsyncClientManager clientManager = new TAsyncClientManager();
    TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
    HelloWorldService.AsyncClient.Factory factory =
        new HelloWorldService.AsyncClient.Factory(clientManager, protocolFactory);
    TNonblockingTransport nonblockingTransport = new TNonblockingSocket("localhost", port);
    HelloWorldService.AsyncClient asyClient = factory.getAsyncClient(nonblockingTransport);
    AsyncMethodCallback<String> callback =
        new AsyncMethodCallback<String>() {
          @Override
          public void onComplete(String s) {
            assertThat(s).isEqualTo("Hello " + "US" + "s' " + "Bob");
            testing()
                .waitAndAssertTraces(
                    trace ->
                        trace.hasSpansSatisfyingExactly(
                            span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                            span ->
                                span.hasName("sayHello")
                                    .hasKind(SpanKind.CLIENT)
                                    .hasParent(trace.getSpan(0))
                                    .hasAttributesSatisfying(
                                        equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                        equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                        equalTo(SemanticAttributes.RPC_METHOD, "sayHello")),
                            span ->
                                span.hasName("sayHello")
                                    .hasKind(SpanKind.SERVER)
                                    .hasParent(trace.getSpan(1))
                                    .hasAttributesSatisfying(
                                        equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                        equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                        equalTo(SemanticAttributes.RPC_METHOD, "sayHello"))));
          }

          @Override
          public void onError(Exception e) {
            fail(e.toString());
          }
        };
    testing().runWithSpan("parent", () -> asyClient.sayHello("US", "Bob", callback));
  }

  @Test
  public void withNoArgs() throws TException {
    startSimpleServer();
    TTransport transport = new TSocket("localhost", port);
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);

    HelloWorldService.Client client = new HelloWorldService.Client(protocol);
    String response = testing().runWithSpan("parent", () -> client.withoutArgs());
    assertThat(response).isEqualTo("no args");
    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName("withoutArgs")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "withoutArgs")),
                    span ->
                        span.hasName("withoutArgs")
                            .hasKind(SpanKind.SERVER)
                            .hasParent(trace.getSpan(1))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "withoutArgs"))));
    transport.close();
  }

  //  @Test
  //  public void withError() throws TException {
  //    startTSimpleServer();
  //    TTransport transport = new TSocket("localhost", port);
  //    transport.open();
  //    TProtocol protocol = new TBinaryProtocol(transport);
  //
  //    HelloWorldService.Client client = new HelloWorldService.Client(protocol);
  //    try{
  //      String response = testing().runWithSpan("parent",()-> client.withError());
  //      assertThat(response).isEqualTo("no args");
  //      fail("get Unexpected response");
  //    }catch (Exception ignore){
  //
  //    }
  //    testing()
  //        .waitAndAssertTraces(trace -> trace.hasSpansSatisfyingExactly(
  //            span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
  //            span -> span.hasName("withError").hasKind(SpanKind.CLIENT)
  //                .hasParent(trace.getSpan(0))
  //                .hasAttributesSatisfying(
  //                    equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
  //                    equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
  //                    equalTo(SemanticAttributes.RPC_METHOD, "withError")
  //                ),
  //            span -> span.hasName("withError").hasKind(SpanKind.SERVER)
  //                .hasParent(trace.getSpan(1))
  //                .hasAttributesSatisfying(
  //                    equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
  //                    equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
  //                    equalTo(SemanticAttributes.RPC_METHOD, "withError")
  //                )
  //        ));
  //    transport.close();
  //  }

  @Test
  public void oneWayAsync() throws TException, IOException {
    startAsyncServer();
    TAsyncClientManager clientManager = new TAsyncClientManager();
    TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
    HelloWorldService.AsyncClient.Factory factory =
        new HelloWorldService.AsyncClient.Factory(clientManager, protocolFactory);
    TNonblockingTransport nonblockingTransport = new TNonblockingSocket("localhost", port);
    HelloWorldService.AsyncClient asyClient = factory.getAsyncClient(nonblockingTransport);
    AsyncMethodCallback<Void> callback =
        new AsyncMethodCallback<Void>() {
          @Override
          public void onComplete(Void response) {
            testing()
                .waitAndAssertTraces(
                    trace ->
                        trace.hasSpansSatisfyingExactly(
                            span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                            span ->
                                span.hasName("oneWay")
                                    .hasKind(SpanKind.CLIENT)
                                    .hasParent(trace.getSpan(0))
                                    .hasAttributesSatisfying(
                                        equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                        equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                        equalTo(SemanticAttributes.RPC_METHOD, "oneWay")),
                            span ->
                                span.hasName("oneWay")
                                    .hasKind(SpanKind.SERVER)
                                    .hasParent(trace.getSpan(1))
                                    .hasAttributesSatisfying(
                                        equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                        equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                        equalTo(SemanticAttributes.RPC_METHOD, "oneWay"))));
          }

          @Override
          public void onError(Exception e) {}
        };
    testing().runWithSpan("parent", () -> asyClient.oneWay(callback));
  }

  @Test
  public void oneWay() throws TException {
    startSimpleServer();
    TTransport transport = new TSocket("localhost", port);
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);

    HelloWorldService.Client client = new HelloWorldService.Client(protocol);
    testing().runWithSpan("parent", () -> client.oneWay());

    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName("oneWay")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "oneWay")),
                    span ->
                        span.hasName("oneWay")
                            .hasKind(SpanKind.SERVER)
                            .hasParent(trace.getSpan(1))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "oneWay"))));
    transport.close();
  }

  @Test
  public void withStruct() throws TException {
    startSimpleServer();
    TTransport transport = new TSocket("localhost", port);
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);
    User user = new User("Bob", "1", 20);
    Account account = new Account("US", "123456");
    HelloWorldService.Client client = new HelloWorldService.Client(protocol);
    UserAccount response = testing().runWithSpan("parent", () -> client.data(user, account));

    assertThat(response.user).isEqualTo(user);
    assertThat(response.account).isEqualTo(account);
    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName("data")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "data")),
                    span ->
                        span.hasName("data")
                            .hasKind(SpanKind.SERVER)
                            .hasParent(trace.getSpan(1))
                            .hasAttributesSatisfying(
                                equalTo(SemanticAttributes.RPC_SYSTEM, "thrift"),
                                equalTo(SemanticAttributes.RPC_SERVICE, "thrift_Service"),
                                equalTo(SemanticAttributes.RPC_METHOD, "data"))));
    transport.close();
  }

  private void startNonBlockingServer() throws TTransportException {
    HelloWorldAsyncImpl impl = new HelloWorldAsyncImpl();
    HelloWorldService.AsyncProcessor<HelloWorldService.AsyncIface> helloServiceProcessor =
        new HelloWorldService.AsyncProcessor<HelloWorldService.AsyncIface>(impl);
    TNonblockingServerTransport transport = new TNonblockingServerSocket(port);
    TThreadedSelectorServer.Args serverArgs =
        new TThreadedSelectorServer.Args(transport)
            .selectorThreads(4)
            .workerThreads(10)
            .acceptQueueSizePerThread(20)
            .processor(helloServiceProcessor);
    server = new TThreadedSelectorServer(serverArgs);
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                server.serve();
              }
            })
        .start();
  }

  private void startAsyncServer() throws TTransportException {
    HelloWorldImpl impl = new HelloWorldImpl();
    // 接口与实现类的绑定关系在这里完成
    HelloWorldService.Processor<HelloWorldImpl> processor =
        new HelloWorldService.Processor<HelloWorldImpl>(impl);
    TNonblockingServerTransport transport = new TNonblockingServerSocket(port);
    TThreadedSelectorServer.Args serverArgs =
        new TThreadedSelectorServer.Args(transport)
            .selectorThreads(4)
            .workerThreads(10)
            .acceptQueueSizePerThread(20)
            .processor(processor);
    server = new TThreadedSelectorServer(serverArgs);
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                server.serve();
              }
            })
        .start();
  }

  private void startSimpleServer() throws TTransportException {
    HelloWorldImpl impl = new HelloWorldImpl();
    // 接口与实现类的绑定关系在这里完成
    HelloWorldService.Processor<HelloWorldImpl> processor =
        new HelloWorldService.Processor<HelloWorldImpl>(impl);
    // 构建服务器

    TServerTransport serverTransport = new TServerSocket(port);
    server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                server.serve();
              }
            })
        .start();
  }

  private void stopServer() {
    if (server != null) {
      server.stop();
    }
  }
}
