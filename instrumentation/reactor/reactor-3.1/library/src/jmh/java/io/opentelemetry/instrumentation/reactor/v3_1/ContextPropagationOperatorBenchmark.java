/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Includes work from:
/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.reactor.v3_1;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
//@Fork(value = 2, warmups = 1, jvmArgsPrepend = "-javaagent:E:\\Temp\\applicationinsights-agent-3.4.13.jar")
@Fork(value = 2, warmups = 1)
@Measurement(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class ContextPropagationOperatorBenchmark {

    private static final AtomicInteger counter = new AtomicInteger(0);

    @Benchmark
    public void WithValidation() {
        simulatedFastIOOperation(true);
    }

    @Benchmark
    public void WithoutValidation() {
        simulatedFastIOOperation(false);
    }

    private static void simulatedFastIOOperation(boolean validateContextPropagation) {
        long sequenceNumber = counter.incrementAndGet();
        String expectedSpanId =  String.valueOf(sequenceNumber);

        Span newSpan = GlobalOpenTelemetry
            .get()
            .getTracerProvider()
            .get("fabianDummy")
            .spanBuilder(expectedSpanId)
            .setAttribute("fabianmDummySeq", expectedSpanId)
            .startSpan();

        io.opentelemetry.context.Scope scope = newSpan.makeCurrent();

        if (validateContextPropagation &&
            !Span.current().getSpanContext().isValid()) {

            throw new IllegalStateException(
                String.format(
                    "Span.current() not valid %s/%s",
                    Span.current().getSpanContext().getTraceId(),
                    Span.current().getSpanContext().getSpanId()));
        }

        try {
            simulatedFastIOOperationCore(expectedSpanId, validateContextPropagation);
        }
        finally {
            newSpan.end();
            scope.close();
        }
    }

    private static void simulatedFastIOOperationCore(String expectedSpanId, boolean validateContextPropagation) {
        Mono<Integer> mono = Mono
            .fromCallable(() -> {

                // TraceContext is required here
                // In subsequent steps of the reactor pipeline we can live in our
                // SDK without TraceContext being propagated
                // What I am looking for is teh Reactor pendant of .Net's Task.ConfigureAwait(false)
                // to suppress synchronization context propagation to optimize perf
                if (validateContextPropagation) {
                    validateTraceContext(expectedSpanId);
                }

                return 1;
            })
            //.publishOn(reactor.core.scheduler.Schedulers.parallel())
            //.delayElement(Duration.ofMillis(1L))
            .map(input -> {
                if (validateContextPropagation) {
                    validateTraceContext(expectedSpanId);
                }
                return input + 1;
            })
            .publishOn(reactor.core.scheduler.Schedulers.elastic())
            .map(input -> {
                if (validateContextPropagation) {
                    validateTraceContext(expectedSpanId);
                }
                return input + 1;
            });


        Integer result = mono
            .subscribeOn(Schedulers.immediate())
            .block();
        if (result == null || 3 != result) {
            throw new IllegalStateException("Unexpected result");
        }
    }

    private static void validateTraceContext(String expectedSpanId) {
        if (expectedSpanId != null) {
            Span currentSpan = Span.current();
            if (currentSpan == null) {
                throw new IllegalStateException("Unexpected trace span - 'currentSpan' should not be null.");
            }

            if (!currentSpan.getSpanContext().isValid()) {
                throw new IllegalStateException("Unexpected trace span - 'currentSpan' should be valid.");
            }

            String expectedValue = expectedSpanId;
            if (!currentSpan.toString().contains("fabianmDummySeq=" + expectedSpanId)) {
                throw new IllegalStateException(
                    String.format(
                        "Unexpected trace span. - unexpected traceId/spanId combination. Expected fragment: %s, Actual span: %s",
                        expectedValue,
                        currentSpan.toString()));
            }
        }
    }
}