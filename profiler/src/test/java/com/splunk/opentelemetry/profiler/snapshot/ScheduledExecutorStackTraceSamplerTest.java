/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.profiler.snapshot;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScheduledExecutorStackTraceSamplerTest {
  private static final Duration HALF_SECOND = Duration.ofMillis(500);
  private static final Duration PERIOD = Duration.ofMillis(20);

  private final IdGenerator idGenerator = IdGenerator.random();
  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final InMemorySpanTracker spanTracker = new InMemorySpanTracker();
  private final ScheduledExecutorStackTraceSampler sampler =
      new ScheduledExecutorStackTraceSampler(staging, spanTracker, PERIOD);

  @Test
  void takeStackTraceSampleForGivenThread() {
    var spanContext = randomSpanContext();

    try {
      sampler.start(spanContext);
      await().atMost(HALF_SECOND).until(() -> !staging.allStackTraces().isEmpty());
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void continuallySampleThreadForStackTraces() {
    var spanContext = randomSpanContext();
    int expectedSamples = (int) HALF_SECOND.dividedBy(PERIOD.multipliedBy(2));

    try {
      sampler.start(spanContext);
      await().atMost(HALF_SECOND).until(() -> staging.allStackTraces().size() >= expectedSamples);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void emptyStagingAreaAfterSamplingStops() {
    var spanContext = randomSpanContext();
    int expectedSamples = (int) HALF_SECOND.dividedBy(PERIOD.multipliedBy(2));

    try {
      sampler.start(spanContext);
      await().atMost(HALF_SECOND).until(() -> staging.allStackTraces().size() >= expectedSamples);
    } finally {
      sampler.stop(spanContext);
    }

    assertEquals(Collections.emptyList(), staging.allStackTraces());
  }

  @Test
  void onlyTakeStackTraceSamplesForOneThreadPerTrace() {
    var executor = Executors.newFixedThreadPool(2);
    var startSpanLatch = new CountDownLatch(1);
    var shutdownLatch = new CountDownLatch(1);
    var traceId = idGenerator.generateTraceId();
    var spanContext2 = randomSpanContext(traceId);
    var spanContext1 = randomSpanContext(traceId);

    executor.submit(startSampling(spanContext1, startSpanLatch, shutdownLatch));
    executor.submit(startSampling(spanContext2, startSpanLatch, shutdownLatch));

    try {
      startSpanLatch.countDown();
      await().until(() -> staging.allStackTraces().size() > 5);
      shutdownLatch.countDown();

      var threadIds =
          staging.allStackTraces().stream()
              .map(StackTrace::getThreadId)
              .collect(Collectors.toSet());
      assertEquals(1, threadIds.size());
    } finally {
      executor.shutdownNow();
      sampler.stop(spanContext1);
      sampler.stop(spanContext2);
    }
  }

  @Test
  void includeTraceIdOnStackTraces() {
    var spanContext = randomSpanContext();
    spanTracker.store(spanContext.getTraceId(), spanContext);

    try {
      sampler.start(spanContext);
      await().atMost(HALF_SECOND).until(() -> !staging.allStackTraces().isEmpty());

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertEquals(spanContext.getTraceId(), stackTrace.getTraceId());
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void includeSpanIdOnStackTraces() {
    var spanContext = randomSpanContext();
    spanTracker.store(spanContext.getTraceId(), spanContext);

    try {
      sampler.start(spanContext);
      await().atMost(HALF_SECOND).until(() -> !staging.allStackTraces().isEmpty());

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertEquals(spanContext.getSpanId(), stackTrace.getSpanId());
    } finally {
      sampler.stop(spanContext);
    }
  }

  private Runnable startSampling(
      SpanContext spanContext, CountDownLatch startSpanLatch, CountDownLatch shutdownLatch) {
    return (() -> {
      try {
        startSpanLatch.await();
        sampler.start(spanContext);
        shutdownLatch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private SpanContext randomSpanContext() {
    return randomSpanContext(idGenerator.generateTraceId());
  }

  private SpanContext randomSpanContext(String traceId) {
    return SpanContext.create(
        traceId, idGenerator.generateSpanId(), TraceFlags.getDefault(), TraceState.getDefault());
  }

  private static class InMemorySpanTracker implements SpanTracker {
    private final Map<String, SpanContext> stackTraces = new HashMap<>();

    void store(String traceId, SpanContext spanContext) {
      stackTraces.put(traceId, spanContext);
    }

    @Override
    public Optional<SpanContext> getActiveSpan(String traceId) {
      return Optional.ofNullable(stackTraces.get(traceId));
    }
  }
}
