/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.thrift;

import io.opentelemetry.javaagent.thrift.thrifttest.Account;
import io.opentelemetry.javaagent.thrift.thrifttest.HelloWorldService;
import io.opentelemetry.javaagent.thrift.thrifttest.User;
import io.opentelemetry.javaagent.thrift.thrifttest.UserAccount;
import java.util.concurrent.TimeUnit;
import org.apache.thrift.TException;

public class HelloWorldImpl implements HelloWorldService.Iface {

  @Override
  public String sayHello(String zone, String name) throws TException {
    return "Hello " + zone + "s' " + name;
  }

  @Override
  public String withDelay(int delay) throws TException {
    try {
      TimeUnit.SECONDS.sleep(delay);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    return "delay " + delay;
  }

  @Override
  public String withoutArgs() throws TException {
    return "no args";
  }

  @Override
  public String withError() throws TException {
    throw new AssertionError("fail");
  }

  @Override
  public String withCollisioin(String input) throws TException {
    return input;
  }

  @Override
  public void oneWay() throws TException {}

  @Override
  public void oneWayWithError() throws TException {
    throw new AssertionError("fail");
  }

  @Override
  public UserAccount data(User user, Account account) throws TException {
    return new UserAccount(user, account);
  }
}
