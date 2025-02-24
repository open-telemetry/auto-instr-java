/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import static io.opentelemetry.javaagent.instrumentation.activejhttp.ActivejHttpServerConnectionSingletons.instrumenter;

import io.activej.http.HttpRequest;
import io.activej.http.HttpResponse;
import io.activej.promise.Promise;
import io.activej.promise.SettablePromise;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class PromiseWrapper {

  public static Promise<HttpResponse> wrap(
      Promise<HttpResponse> promise, HttpRequest httpRequest, Context context) {
    SettablePromise<HttpResponse> settablePromise = new SettablePromise<>();
    promise
        .whenResult(
            result -> {
              Exception error = null;
              try (Scope ignored = context.makeCurrent()) {
                settablePromise.set(result);
              } catch (RuntimeException exception) {
                error = exception;
                settablePromise.setException(
                    new RuntimeException("Context management failed", exception));
              } finally {
                instrumenter().end(context, httpRequest, result, error);
              }
            })
        .whenException(
            throwable -> {
              try (Scope ignored = context.makeCurrent()) {
                settablePromise.setException(throwable);
              } catch (RuntimeException exception) {
                settablePromise.setException(
                    new RuntimeException("Context management failed", exception));
              } finally {
                instrumenter().end(context, httpRequest, null, throwable);
              }
            });
    return settablePromise;
  }

  private PromiseWrapper() {}
}
