/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskWithError implements Runnable {

  private final CountDownLatch latch = new CountDownLatch(1);

  @Scheduled(fixedRate = 5000)
  @Override
  public void run() {
    latch.countDown();
    throw new IllegalStateException("failure");
  }

  public void blockUntilExecute() throws InterruptedException {
    latch.await(5, TimeUnit.SECONDS);
  }
}
