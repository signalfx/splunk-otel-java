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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScheduledExecutorStackTraceSamplerTest {
  private static final Duration SAMPLING_PERIOD = Duration.ofMillis(20);

  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final InMemorySpanTracker spanTracker = new InMemorySpanTracker();
  private final ScheduledExecutorStackTraceSampler sampler =
      new ScheduledExecutorStackTraceSampler(staging, () -> spanTracker, SAMPLING_PERIOD);

  @Test
  void takeStackTraceSampleForGivenThread() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(spanContext);
      await().until(() -> staging.hasStackTraces(spanContext));
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void continuallySampleThreadForStackTraces() {
    var halfSecond = Duration.ofMillis(500);
    var spanContext = Snapshotting.spanContext().build();
    int expectedSamples = (int) halfSecond.dividedBy(SAMPLING_PERIOD.multipliedBy(2));

    try {
      sampler.start(spanContext);
      await().until(() -> staging.getStackTraces(spanContext).size() >= expectedSamples);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void emptyStagingAreaAfterSamplingStops() {
    var halfSecond = Duration.ofMillis(500);
    var spanContext = Snapshotting.spanContext().build();
    int expectedSamples = (int) halfSecond.dividedBy(SAMPLING_PERIOD.multipliedBy(2));

    try {
      sampler.start(spanContext);
      await().until(() -> staging.getStackTraces(spanContext).size() >= expectedSamples);
    } finally {
      sampler.stop(spanContext);
    }

    assertThat(staging.getStackTraces(spanContext).emptied()).isTrue();
  }

  @Test
  void onlyTakeStackTraceSamplesForOneThreadPerTrace() {
    var executor = Executors.newFixedThreadPool(2);
    var startSpanLatch = new CountDownLatch(1);
    var shutdownLatch = new CountDownLatch(1);
    var traceId = IdGenerator.random().generateTraceId();
    var spanContext2 = Snapshotting.spanContext().withTraceId(traceId).build();
    var spanContext1 = Snapshotting.spanContext().withTraceId(traceId).build();

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
  void includeTimestampOnStackTraces() {
    var now = Instant.now();
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(spanContext);
      await().until(() -> staging.hasStackTraces(spanContext));

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertThat(stackTrace.getTimestamp()).isNotNull().isAfter(now);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void includeSamplingPeriodOnFirstRecordedStackTraces() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(spanContext);
      await().until(() -> staging.hasStackTraces(spanContext));

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertThat(stackTrace.getDuration()).isNotNull().isGreaterThan(Duration.ZERO);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void calculateSamplingPeriodAfterFirstRecordedStackTraces() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(spanContext);
      await().until(() -> staging.getStackTraces(spanContext).size() > 1);

      var stackTrace = staging.getStackTraces(spanContext).stream().skip(1).findFirst().orElseThrow();
      assertThat(stackTrace.getDuration()).isNotNull().isCloseTo(SAMPLING_PERIOD, Duration.ofMillis(5));
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void includeThreadDetailsOnStackTraces() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    var spanContext = Snapshotting.spanContext().build();
    var startLatch = new CountDownLatch(1);
    var stopLatch = new CountDownLatch(1);
    try {
      var future = executor.submit(startSampling(spanContext, startLatch, stopLatch));

      startLatch.countDown();
      await().until(() -> staging.hasStackTraces(spanContext));
      stopLatch.countDown();

      var thread = future.get();
      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertAll(
          () -> assertEquals(thread.id, stackTrace.getThreadId()),
          () -> assertEquals(thread.name, stackTrace.getThreadName()),
          () -> assertNotNull(stackTrace.getThreadState()),
          () -> assertThat(stackTrace.getStackFrames()).isNotEmpty());
    } finally {
      sampler.stop(spanContext);
      executor.shutdownNow();
    }
  }

  @Test
  void includeTraceIdOnStackTraces() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(spanContext);
      await().until(() -> staging.hasStackTraces(spanContext));

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertEquals(spanContext.getTraceId(), stackTrace.getTraceId());
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void includeActiveSpanIdOnStackTraces() {
    var spanContext = Snapshotting.spanContext().build();
    spanTracker.store(Thread.currentThread().getId(), spanContext);

    try {
      sampler.start(spanContext);
      await().until(() -> staging.hasStackTraces(spanContext));

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertEquals(spanContext.getSpanId(), stackTrace.getSpanId());
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void takeFinalSampleWhenTraceSamplingIsStopped() {
    var scheduler = Executors.newScheduledThreadPool(2);
    var spanContext = Snapshotting.spanContext().build();
    var startLatch = new CountDownLatch(0);
    var stopLatch = new CountDownLatch(1);
    var expectedDuration = SAMPLING_PERIOD.dividedBy(2);
    try {
      scheduler.submit(startSampling(spanContext, startLatch, stopLatch));
      scheduler.schedule(() -> sampler.stop(spanContext), expectedDuration.toMillis(), TimeUnit.MILLISECONDS);
      await().until(() -> staging.hasStackTraces(spanContext));
      stopLatch.countDown();

      var stackTraces = staging.allStackTraces();
      assertEquals(2, stackTraces.size());
    } finally {
      scheduler.shutdownNow();
    }
  }

  @Test
  void finalSampleDurationIsLessSmallerThanSamplingPeriod() {
    var scheduler = Executors.newScheduledThreadPool(2);
    var spanContext = Snapshotting.spanContext().build();
    var startLatch = new CountDownLatch(0);
    var stopLatch = new CountDownLatch(1);
    var expectedDuration = SAMPLING_PERIOD.dividedBy(2);
    try {
      scheduler.submit(startSampling(spanContext, startLatch, stopLatch));
      scheduler.schedule(() -> sampler.stop(spanContext), expectedDuration.toMillis(), TimeUnit.MILLISECONDS);
      await().until(() -> staging.hasStackTraces(spanContext));
      stopLatch.countDown();

      var stackTraces = staging.getStackTraces(spanContext);
      var lastStackTrace = stackTraces.get(stackTraces.size() - 1);
      assertThat(lastStackTrace.getDuration()).isLessThan(SAMPLING_PERIOD);
    } finally {
      scheduler.shutdownNow();
    }
  }

  private Callable<ThreadInfo> startSampling(
      SpanContext spanContext, CountDownLatch startSpanLatch, CountDownLatch shutdownLatch) {
    return (() -> {
      try {
        startSpanLatch.await();
        sampler.start(spanContext);
        shutdownLatch.await();
        return new ThreadInfo(Thread.currentThread().getId(), Thread.currentThread().getName());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static class ThreadInfo {
    public final long id;
    public final String name;

    private ThreadInfo(long id, String name) {
      this.id = id;
      this.name = name;
    }
  }
}
