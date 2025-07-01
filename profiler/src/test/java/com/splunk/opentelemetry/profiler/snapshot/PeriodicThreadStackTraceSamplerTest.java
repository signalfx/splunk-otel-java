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
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PeriodicThreadStackTraceSamplerTest {
  private static final Duration SAMPLING_PERIOD = Duration.ofMillis(20);

  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final InMemorySpanTracker spanTracker = new InMemorySpanTracker();
  private final DelayedThreadInfoCollector delayedThreadInfoCollector =
      new DelayedThreadInfoCollector();
  private final PeriodicThreadStackTraceSampler sampler =
      new PeriodicThreadStackTraceSampler(
          () -> staging, () -> spanTracker, delayedThreadInfoCollector, SAMPLING_PERIOD);

  @AfterEach
  void tearDown() {
    sampler.close();
  }

  @Test
  void takeStackTraceSampleForGivenThread() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(spanContext);
      await().until(staging::hasStackTraces);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void continuallySampleThreadForStackTraces() {
    var spanContext = Snapshotting.spanContext().build();
    var halfSecond = Duration.ofMillis(500);
    int expectedSamples = (int) halfSecond.dividedBy(SAMPLING_PERIOD.multipliedBy(2));

    try {
      sampler.start(spanContext);
      await().until(() -> staging.allStackTraces().size() >= expectedSamples);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void onlyTakeStackTraceSamplesForOneThreadPerTrace() {
    var executor = Executors.newFixedThreadPool(2);
    var control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var traceId = IdGenerator.random().generateTraceId();
    var spanContext2 = Snapshotting.spanContext().withTraceId(traceId).build();
    var spanContext1 = Snapshotting.spanContext().withTraceId(traceId).build();

    executor.submit(startSampling(spanContext1, control));
    executor.submit(startSampling(spanContext2, control));

    try {
      control.start();
      await().until(() -> staging.allStackTraces().size() > 5);
      control.stop();

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

  /**
   * Attempting to exercise the following scenario. <br>
   * <br>
   * Two services: A and B where A makes multiple API calls into B in parallel. If the trace is
   * profiled the first API call into B will start profiling within B, and the profiling should not
   * stop until that same span ends.
   */
  @Test
  void onlyStopSamplingWhenCommandOriginatesSameSpanContextThatStartedSampling() {
    var executor = Executors.newFixedThreadPool(2);
    var traceId = IdGenerator.random().generateTraceId();

    var thread1Control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var spanContext1 = Snapshotting.spanContext().withTraceId(traceId).build();

    var thread2Control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var spanContext2 = Snapshotting.spanContext().withTraceId(traceId).build();

    executor.submit(startSampling(spanContext1, thread1Control));
    executor.submit(startSampling(spanContext2, thread2Control));

    try {
      thread1Control.start();
      await().until(() -> staging.allStackTraces().size() > 1);
      thread2Control.start();
      await().until(() -> staging.allStackTraces().size() > 2);

      thread2Control.stop();
      int numberOfStackTraces = staging.allStackTraces().size();
      sampler.stop(spanContext2);

      await()
          .untilAsserted(
              () -> assertThat(staging.allStackTraces().size()).isGreaterThan(numberOfStackTraces));
    } finally {
      sampler.stop(spanContext1);
      thread1Control.stop();
      executor.shutdownNow();
    }
  }

  @Test
  void sampleMultipleTracesAtSameTime() {
    var executor = Executors.newFixedThreadPool(2);
    var control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var spanContext2 = Snapshotting.spanContext().build();
    var spanContext1 = Snapshotting.spanContext().build();

    executor.submit(startSampling(spanContext1, control));
    executor.submit(startSampling(spanContext2, control));

    try {
      control.start();
      await().until(() -> staging.allStackTraces().size() > 10);
      control.stop();

      var threadIds =
          staging.allStackTraces().stream()
              .map(StackTrace::getThreadId)
              .collect(Collectors.toSet());
      assertEquals(2, threadIds.size());

      var traceIds =
          staging.allStackTraces().stream().map(StackTrace::getTraceId).collect(Collectors.toSet());
      assertThat(traceIds).contains(spanContext1.getTraceId(), spanContext2.getTraceId());
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
      await().until(staging::hasStackTraces);

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
      await().until(staging::hasStackTraces);

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
      await().until(() -> staging.allStackTraces().size() > 1);

      var stackTrace = staging.allStackTraces().stream().skip(1).findFirst().orElseThrow();
      assertThat(stackTrace.getDuration())
          .isNotNull()
          .isCloseTo(SAMPLING_PERIOD, Duration.ofMillis(6));
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void includeThreadDetailsOnStackTraces() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    var spanContext = Snapshotting.spanContext().build();
    var control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    try {
      var future = executor.submit(startSampling(spanContext, control));

      control.start();
      await().until(staging::hasStackTraces);
      control.stop();

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
      await().until(staging::hasStackTraces);

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
      await().until(staging::hasStackTraces);

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertEquals(spanContext.getSpanId(), stackTrace.getSpanId());
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void takeInitialSampleWhenTraceSamplingStarts() {
    var scheduler = Executors.newScheduledThreadPool(2);
    var spanContext = Snapshotting.spanContext().build();
    var control = new ThreadControl(new CountDownLatch(0), new CountDownLatch(1));
    var expectedDuration = SAMPLING_PERIOD.dividedBy(2);
    try {
      scheduler.submit(startSampling(spanContext, control));
      scheduler.schedule(
          () -> sampler.stop(spanContext), expectedDuration.toMillis(), TimeUnit.MILLISECONDS);
      await().until(staging::hasStackTraces);
      control.stop.countDown();

      var stackTraces = staging.allStackTraces();
      assertEquals(2, stackTraces.size());
    } finally {
      scheduler.shutdownNow();
    }
  }

  @Test
  void aTest() {
    delayedThreadInfoCollector.setDelay(Duration.ofMillis(100));

    var spanContext1 = Snapshotting.spanContext().build();
    var spanContext2 = Snapshotting.spanContext().build();
    sampler.start(spanContext1);
    spanTracker.store(Thread.currentThread().getId(), spanContext1);
    sampler.stop(spanContext1);
    sampler.start(spanContext2);
    spanTracker.store(Thread.currentThread().getId(), spanContext2);

    await().until(() -> staging.allStackTraces().size() > 1);

    System.out.println(
        "SpanContext: " + spanContext1.getTraceId() + "|" + spanContext1.getSpanId());
    System.out.println(
        "SpanContext: " + spanContext2.getTraceId() + "|" + spanContext2.getSpanId());
    staging
        .allStackTraces()
        .forEach(s -> System.out.println("StackTrace: " + s.getTraceId() + "|" + s.getSpanId()));

    assertThat(staging.allStackTraces())
        .map(
            st ->
                Snapshotting.spanContext()
                    .withTraceId(st.getTraceId())
                    .withSpanId(st.getSpanId())
                    .build())
        .contains(spanContext1);
  }

  @Test
  void takeFinalSampleWhenTraceSamplingIsStopped() {
    var scheduler = Executors.newScheduledThreadPool(2);
    var spanContext = Snapshotting.spanContext().build();
    var control = new ThreadControl(new CountDownLatch(0), new CountDownLatch(1));
    var expectedDuration = SAMPLING_PERIOD.dividedBy(2);
    try {
      scheduler.submit(startSampling(spanContext, control));
      scheduler.schedule(
          () -> sampler.stop(spanContext), expectedDuration.toMillis(), TimeUnit.MILLISECONDS);
      await().until(staging::hasStackTraces);
      control.stop.countDown();

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
    var controls = new ThreadControl(new CountDownLatch(0), new CountDownLatch(1));
    var expectedDuration = SAMPLING_PERIOD.dividedBy(2);
    try {
      scheduler.submit(startSampling(spanContext, controls));
      scheduler.schedule(
          () -> sampler.stop(spanContext), expectedDuration.toMillis(), TimeUnit.MILLISECONDS);
      await().until(staging::hasStackTraces);
      controls.stop.countDown();

      var stackTraces = staging.allStackTraces();
      var lastStackTrace = stackTraces.get(stackTraces.size() - 1);
      assertThat(lastStackTrace.getDuration()).isLessThan(SAMPLING_PERIOD);
    } finally {
      scheduler.shutdownNow();
    }
  }

  private Callable<ThreadInfo> startSampling(SpanContext spanContext, ThreadControl control) {
    return (() -> {
      try {
        control.start.await();
        sampler.start(spanContext);
        control.stop.await();
        return new ThreadInfo(Thread.currentThread());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static class ThreadControl {
    private final CountDownLatch start;
    private final CountDownLatch stop;

    private ThreadControl(CountDownLatch start) {
      this(start, new CountDownLatch(0));
    }

    private ThreadControl(CountDownLatch start, CountDownLatch stop) {
      this.start = start;
      this.stop = stop;
    }

    void start() {
      start.countDown();
    }

    void stop() {
      stop.countDown();
    }
  }

  private static class ThreadInfo {
    public final long id;
    public final String name;

    private ThreadInfo(Thread thread) {
      this.id = thread.getId();
      this.name = thread.getName();
    }
  }

  @Test
  void stopSamplingWhenClosed() throws Exception {
    var spanContext = Snapshotting.spanContext().build();

    sampler.start(spanContext);
    await().until(() -> !staging.allStackTraces().isEmpty());
    sampler.close();

    staging.empty();

    var scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      var future =
          scheduler.schedule(
              reportStackTracesStaged(), SAMPLING_PERIOD.toMillis() * 10, TimeUnit.MILLISECONDS);
      assertEquals(0, future.get());
    } finally {
      scheduler.shutdownNow();
    }
  }

  @Test
  void stopSamplingForEveryTraceWhenClosed() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    var control = new ThreadControl(new CountDownLatch(1));
    var spanContext2 = Snapshotting.spanContext().build();
    var spanContext1 = Snapshotting.spanContext().build();

    executor.submit(startSampling(spanContext1, control));
    executor.submit(startSampling(spanContext2, control));
    try {
      control.start();
      await().until(() -> staging.allStackTraces().size() > 5);
      sampler.close();
      staging.empty();

      var future =
          scheduler.schedule(
              reportStackTracesStaged(), SAMPLING_PERIOD.toMillis() * 10, TimeUnit.MILLISECONDS);
      assertEquals(0, future.get());
    } finally {
      executor.shutdownNow();
      scheduler.shutdownNow();
    }
  }

  private Callable<Integer> reportStackTracesStaged() {
    return () -> staging.allStackTraces().size();
  }

  private static class DelayedThreadInfoCollector extends ThreadInfoCollector {
    private Duration delay = Duration.ofMillis(0);

    void setDelay(Duration delay) {
      this.delay = delay;
    }

    @Override
    java.lang.management.ThreadInfo getThreadInfo(long threadId) {
      try {
        Thread.sleep(delay.toMillis());
        return super.getThreadInfo(threadId);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    java.lang.management.ThreadInfo[] getThreadInfo(Collection<Long> threadIds) {
      try {
        Thread.sleep(delay.toMillis());
        return super.getThreadInfo(threadIds);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
