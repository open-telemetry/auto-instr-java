/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.thrift;

import com.google.common.base.VerifyException;
import io.opentelemetry.javaagent.thrift.thrifttest.Account;
import io.opentelemetry.javaagent.thrift.thrifttest.HelloWorldService;
import io.opentelemetry.javaagent.thrift.thrifttest.User;
import io.opentelemetry.javaagent.thrift.thrifttest.UserAccount;
import java.util.concurrent.TimeUnit;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

public class HelloWorldAsyncImpl implements HelloWorldService.AsyncIface {
  @Override
  public void sayHello(String zone, String name, AsyncMethodCallback<String> resultHandler)
      throws TException {
    resultHandler.onComplete("Hello " + zone + "s' " + name);
  }

  @Override
  public void withDelay(int delay, AsyncMethodCallback<String> resultHandler) throws TException {
    try {
      TimeUnit.SECONDS.sleep(delay);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    resultHandler.onComplete("delay " + delay);
  }

  @Override
  public void withoutArgs(AsyncMethodCallback<String> resultHandler) throws TException {
    resultHandler.onComplete("no args");
  }

  @Override
  public void withError(AsyncMethodCallback<String> resultHandler) throws TException {
    throw new AssertionError("fail");
  }

  @Override
  public void withCollisioin(String input, AsyncMethodCallback<String> resultHandler)
      throws TException {
    resultHandler.onComplete(input);
  }

  @Override
  public void oneWay(AsyncMethodCallback<Void> resultHandler) throws TException {}

  @Override
  public void oneWayWithError(AsyncMethodCallback<Void> resultHandler) throws TException {
    throw new VerifyException("fail");
  }

  @Override
  public void data(User user, Account account, AsyncMethodCallback<UserAccount> resultHandler)
      throws TException {
    resultHandler.onComplete(new UserAccount(user, account));
    ;
  }
}
