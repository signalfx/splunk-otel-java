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
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class PeriodicStackTraceSamplerTest {
  private static final Duration SAMPLING_PERIOD = Duration.ofMillis(20);

  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final InMemorySpanTracker spanTracker = new InMemorySpanTracker();
  private final DelayedThreadInfoCollector delayedThreadInfoCollector =
      new DelayedThreadInfoCollector();
  private final PeriodicStackTraceSampler sampler =
      new PeriodicStackTraceSampler(
          () -> staging, () -> spanTracker, delayedThreadInfoCollector, SAMPLING_PERIOD);

  @AfterEach
  void tearDown() {
    sampler.close();
  }

  @Test
  void takeStackTraceSampleForThread() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(Thread.currentThread(), spanContext);
      await().until(staging::hasStackTraces);
    } finally {
      sampler.stop(Thread.currentThread());
    }
  }

  @Test
  void continuallySampleThreadForStackTraces() {
    var spanContext = Snapshotting.spanContext().build();
    var halfSecond = Duration.ofMillis(500);
    int expectedSamples = (int) halfSecond.dividedBy(SAMPLING_PERIOD.multipliedBy(2));

    try {
      sampler.start(Thread.currentThread(), spanContext);
      await().until(() -> staging.allStackTraces().size() >= expectedSamples);
    } finally {
      sampler.stop(Thread.currentThread());
    }
  }

  @Test
  void takeStackTraceSamplesForMultipleThreadsFromSameTrace() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var traceId = IdGenerator.random().generateTraceId();
    var spanContext2 = Snapshotting.spanContext().withTraceId(traceId).build();
    var spanContext1 = Snapshotting.spanContext().withTraceId(traceId).build();

    var thread1 = executor.submit(captureThread(startSampling(spanContext1, control)));
    var thread2 = executor.submit(captureThread(startSampling(spanContext2, control)));

    try {
      control.start();
      await().until(() -> staging.allStackTraces().size() > 5);
      control.stop();

      var threadIds =
          staging.allStackTraces().stream()
              .map(StackTrace::getThreadId)
              .collect(Collectors.toSet());
      assertEquals(2, threadIds.size());
    } finally {
      sampler.stop(thread1.get());
      sampler.stop(thread2.get());
      executor.shutdownNow();
    }
  }

  @Test
  void takeStackTraceSamplesForMultipleThreadsFromSameSpan() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var traceId = IdGenerator.random().generateTraceId();
    var spanContext = Snapshotting.spanContext().withTraceId(traceId).build();

    var thread1 = executor.submit(captureThread(startSampling(spanContext, control)));
    var thread2 = executor.submit(captureThread(startSampling(spanContext, control)));

    try {
      control.start();
      await().until(() -> staging.allStackTraces().size() > 5);
      control.stop();

      var threadIds = Set.of(thread1.get().getId(), thread2.get().getId());
      var profiledThreads =
          staging.allStackTraces().stream()
              .map(StackTrace::getThreadId)
              .collect(Collectors.toSet());
      assertEquals(threadIds, profiledThreads);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void sampleMultipleTracesAtSameTime() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var spanContext2 = Snapshotting.spanContext().build();
    var spanContext1 = Snapshotting.spanContext().build();

    var thread1 = executor.submit(captureThread(startSampling(spanContext1, control)));
    var thread2 = executor.submit(captureThread(startSampling(spanContext2, control)));

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
      sampler.stop(thread1.get());
      sampler.stop(thread2.get());
      executor.shutdownNow();
    }
  }

  @Test
  void includeTimestampOnStackTraces() {
    var now = Instant.now();
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(Thread.currentThread(), spanContext);
      await().until(staging::hasStackTraces);

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertThat(stackTrace.getTimestamp()).isNotNull().isAfter(now);
    } finally {
      sampler.stop(Thread.currentThread());
    }
  }

  @Test
  void includeSamplingPeriodOnFirstRecordedStackTraces() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(Thread.currentThread(), spanContext);
      await().until(staging::hasStackTraces);

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertThat(stackTrace.getDuration()).isNotNull().isGreaterThan(Duration.ZERO);
    } finally {
      sampler.stop(Thread.currentThread());
    }
  }

  @Test
  void calculateSamplingPeriodAfterFirstRecordedStackTraces() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(Thread.currentThread(), spanContext);
      await().until(() -> staging.allStackTraces().size() > 1);

      var stackTrace = staging.allStackTraces().stream().skip(1).findFirst().orElseThrow();
      assertThat(stackTrace.getDuration())
          .isNotNull()
          .isCloseTo(SAMPLING_PERIOD, Duration.ofMillis(6));
    } finally {
      sampler.stop(Thread.currentThread());
    }
  }

  @Test
  void includeThreadDetailsOnStackTraces() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    var spanContext = Snapshotting.spanContext().build();
    var control = new ThreadControl(new CountDownLatch(1), new CountDownLatch(1));
    var thread = executor.submit(captureThread(startSampling(spanContext, control)));
    try {
      control.start();
      await().until(staging::hasStackTraces);
      control.stop();

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertAll(
          () -> assertEquals(thread.get().getId(), stackTrace.getThreadId()),
          () -> assertEquals(thread.get().getName(), stackTrace.getThreadName()),
          () -> assertNotNull(stackTrace.getThreadState()),
          () -> assertThat(stackTrace.getStackFrames()).isNotEmpty());
    } finally {
      sampler.stop(thread.get());
      executor.shutdownNow();
    }
  }

  @Test
  void includeTraceIdOnStackTraces() {
    var spanContext = Snapshotting.spanContext().build();

    try {
      sampler.start(Thread.currentThread(), spanContext);
      await().until(staging::hasStackTraces);

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertEquals(spanContext.getTraceId(), stackTrace.getTraceId());
    } finally {
      sampler.stop(Thread.currentThread());
    }
  }

  @Test
  void includeActiveSpanIdOnStackTraces() {
    var spanContext = Snapshotting.spanContext().build();
    spanTracker.store(Thread.currentThread(), spanContext);

    try {
      sampler.start(Thread.currentThread(), spanContext);
      await().until(staging::hasStackTraces);

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertEquals(spanContext.getSpanId(), stackTrace.getSpanId());
    } finally {
      sampler.stop(Thread.currentThread());
    }
  }

  @Test
  void takeInitialSampleWhenTraceSamplingStarts() {
    var spanContext = Snapshotting.spanContext().build();
    sampler.start(Thread.currentThread(), spanContext);
    assertEquals(1, staging.allStackTraces().size());
  }

  @Test
  void ensureStartAndStopSamplesAreAssociatedWithCorrectTraceAndSpanId() {
    // When start and stop samples are taken on a background thread, this delay
    // will cause the thread to be associated with a different trace. The result is
    // a StackTrace with the expected trace id but a span id from a different trace.
    delayedThreadInfoCollector.setDelay(Duration.ofMillis(100));

    var thread = Thread.currentThread();
    var spanContext1 = Snapshotting.spanContext().build();
    var spanContext2 = Snapshotting.spanContext().build();

    sampler.start(thread, spanContext1);
    spanTracker.store(thread, spanContext1);
    sampler.stop(thread);
    sampler.start(thread, spanContext2);
    spanTracker.store(thread, spanContext2);

    await().until(() -> staging.allStackTraces().size() > 1);

    assertThat(staging.allStackTraces())
        .map(
            st ->
                Snapshotting.spanContext()
                    .withTraceId(st.getTraceId())
                    .withSpanId(st.getSpanId())
                    .build())
        .contains(spanContext1);
  }

  /**
   * This test is attempting to model a scenario where trace sampling is stopped, but the background
   * sampling thread still has that trace id in its view. Specifically the test is attempting to
   * construct the following sequence.
   *
   * <p>T1 - sampler.start(Trace 1 on Thread 1)
   *
   * <p>At some point later but roughly the same time. <br>
   * T2 - sampler.stop(Trace 1 on Thread 1) <br>
   * T2 - sampler.sample(All threads, including Thread 1)
   *
   * <p>The assumption is sampler.sample() will include Thread 1 in its view and retrieve details
   * about the thread. If this happens we do not want Thread 1 to have a {@link StackTrace} recorded
   * by the background sampling thread.
   *
   * <p>Since we can't fully coordinate the threads to consistently reproduce the scenario we repeat
   * the test a handful of times to increase the odds the race condition will be encountered.
   */
  @RepeatedTest(10)
  void doNotStageStackTraceWhenThreadNoLongerAssociatedWithSameTraceId() throws Exception {
    var spanContext = Snapshotting.spanContext().build();
    var latch = new CountDownLatch(1);
    var collector = new CoordinatingThreadInfoCollector(latch);

    var thread1 = Executors.newScheduledThreadPool(1);
    var thread2 = Executors.newScheduledThreadPool(1);
    try (var sampler =
        new PeriodicStackTraceSampler(
            () -> staging, () -> spanTracker, collector, SAMPLING_PERIOD)) {
      thread1.submit(() -> sampler.start(Thread.currentThread(), spanContext)).get();

      collector.blockThreadInfoCollection();

      // wait for the start span snapshot remove it
      await().until(() -> !staging.allStackTraces().isEmpty());
      staging.empty();

      var future = thread1.submit(captureThread(() -> sampler.stop(Thread.currentThread())));
      thread2.schedule(latch::countDown, SAMPLING_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
      await().until(() -> !staging.allStackTraces().isEmpty());

      var thread = future.get();
      assertEquals(1, staging.allStackTraces().size());
      assertEquals(thread.getId(), staging.allStackTraces().get(0).getRecordingThreadId());
    } finally {
      thread1.shutdownNow();
      thread2.shutdownNow();
    }
  }

  /**
   * A negative samping period means one of the two competing threads started the sampling first but
   * ultimately lost the race. The "winning" sample's duration fully encompasses the "losing" sample
   * and the "losing" sample's duration will be negative.
   *
   * <p>Since we can't fully coordinate the threads to consistently reproduce the scenario, we
   * repeat the test a handful of times to increase the odds the race condition will be encountered.
   */
  @RepeatedTest(10)
  void dropSamplesWithNegativeSamplingPeriods() throws Exception {
    var spanContext = Snapshotting.spanContext().build();
    var latch = new CountDownLatch(1);
    var collector = new CoordinatingThreadInfoCollector(latch);

    var thread1 = Executors.newScheduledThreadPool(1);
    var thread2 = Executors.newScheduledThreadPool(1);
    try (var sampler =
        new PeriodicStackTraceSampler(
            () -> staging, () -> spanTracker, collector, SAMPLING_PERIOD)) {
      thread1.submit(() -> sampler.start(Thread.currentThread(), spanContext)).get();

      collector.blockThreadInfoCollection();

      // wait for the start span snapshot remove it
      await().until(() -> !staging.allStackTraces().isEmpty());
      staging.empty();

      thread1.submit(() -> sampler.stop(Thread.currentThread()));
      thread2.schedule(latch::countDown, SAMPLING_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
      await().until(() -> !staging.allStackTraces().isEmpty());

      assertThat(staging.allStackTraces())
          .map(StackTrace::getDuration)
          .noneMatch(Duration::isNegative);
    } finally {
      thread1.shutdownNow();
      thread2.shutdownNow();
    }
  }

  @Test
  void takeFinalSampleWhenTraceSamplingIsStopped() {
    var spanContext = Snapshotting.spanContext().build();

    sampler.start(Thread.currentThread(), spanContext);
    sampler.stop(Thread.currentThread());

    var stackTraces = staging.allStackTraces();
    assertEquals(2, stackTraces.size());
  }

  @Test
  void finalSampleDurationIsLessThanSamplingPeriod() {
    var scheduler = Executors.newScheduledThreadPool(1);
    var spanContext = Snapshotting.spanContext().build();
    var expectedDuration = SAMPLING_PERIOD.dividedBy(2);
    try {
      scheduler.submit(startSampling(spanContext));
      scheduler.schedule(stopSampling(), expectedDuration.toMillis(), TimeUnit.MILLISECONDS);
      await().until(staging::hasStackTraces);

      var stackTraces = staging.allStackTraces();
      var lastStackTrace = stackTraces.get(stackTraces.size() - 1);
      assertThat(lastStackTrace.getDuration()).isLessThan(SAMPLING_PERIOD);
    } finally {
      scheduler.shutdownNow();
    }
  }

  private Runnable stopSampling() {
    return () -> sampler.stop(Thread.currentThread());
  }

  private static class ThreadControl {
    private final CountDownLatch start;
    private final CountDownLatch stop;

    private ThreadControl() {
      this(new CountDownLatch(0), new CountDownLatch(0));
    }

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

  @Test
  void stopSamplingWhenClosed() throws Exception {
    var spanContext = Snapshotting.spanContext().build();

    sampler.start(Thread.currentThread(), spanContext);
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

  @Test
  void doNotAcceptNewTracesForSamplingWhenClosed() {
    var spanContext = Snapshotting.spanContext().build();

    sampler.close();
    sampler.start(Thread.currentThread(), spanContext);

    assertEquals(0, staging.allStackTraces().size());
  }

  @Test
  void doNotReportStackTraceForStoppingSamplingWhenClosed() {
    var spanContext = Snapshotting.spanContext().build();

    sampler.start(Thread.currentThread(), spanContext);
    sampler.close();
    staging.empty();

    sampler.stop(Thread.currentThread());

    assertEquals(0, staging.allStackTraces().size());
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
    ThreadInfo getThreadInfo(long threadId) {
      try {
        Thread.sleep(delay.toMillis());
        return super.getThreadInfo(threadId);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    ThreadInfo[] getThreadInfo(Collection<Long> threadIds) {
      try {
        Thread.sleep(delay.toMillis());
        return super.getThreadInfo(threadIds);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class CoordinatingThreadInfoCollector extends ThreadInfoCollector {
    private final AtomicBoolean wait = new AtomicBoolean(false);
    private final CountDownLatch latch;

    private CoordinatingThreadInfoCollector(CountDownLatch latch) {
      this.latch = latch;
    }

    void blockThreadInfoCollection() {
      wait.set(true);
    }

    @Override
    ThreadInfo getThreadInfo(long threadId) {
      try {
        var ti = super.getThreadInfo(threadId);
        if (wait.get()) {
          latch.await();
        }
        return ti;
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    ThreadInfo[] getThreadInfo(Collection<Long> threadIds) {
      try {
        var tis = super.getThreadInfo(threadIds);
        if (wait.get()) {
          latch.await();
        }
        return tis;
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  void threadIsNotBeingSampled() {
    assertThat(sampler.isBeingSampled(Thread.currentThread())).isFalse();
  }

  @Test
  void threadIsBeingSampled() {
    var spanContext = Snapshotting.spanContext().build();
    sampler.start(Thread.currentThread(), spanContext);

    assertThat(sampler.isBeingSampled(Thread.currentThread())).isTrue();
  }

  @Test
  void multipleThreadsAreBeingSampled() throws Exception {
    var spanContext = Snapshotting.spanContext().build();
    var executor = Executors.newFixedThreadPool(2);
    try {
      var thread1 = executor.submit(captureThread(startSampling(spanContext)));
      var thread2 = executor.submit(captureThread(startSampling(spanContext)));

      assertThat(sampler.isBeingSampled(thread1.get())).isTrue();
      assertThat(sampler.isBeingSampled(thread2.get())).isTrue();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void threadIsNotBeingSampledAfterSamplingIsStopped() {
    var thread = Thread.currentThread();
    var spanContext = Snapshotting.spanContext().build();

    sampler.start(thread, spanContext);
    sampler.stop(thread);

    assertThat(sampler.isBeingSampled(thread)).isFalse();
  }

  private Runnable startSampling(SpanContext spanContext) {
    return startSampling(spanContext, new ThreadControl());
  }

  private Runnable startSampling(SpanContext spanContext, ThreadControl control) {
    return (() -> {
      try {
        control.start.await();
        sampler.start(Thread.currentThread(), spanContext);
        control.stop.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private Callable<Thread> captureThread(Runnable runnable) {
    return () -> {
      runnable.run();
      return Thread.currentThread();
    };
  }
}
