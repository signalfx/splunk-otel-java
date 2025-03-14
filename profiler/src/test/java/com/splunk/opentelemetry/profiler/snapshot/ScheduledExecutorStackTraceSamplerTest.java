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
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScheduledExecutorStackTraceSamplerTest {
  private static final Duration HALF_SECOND = Duration.ofMillis(500);
  private static final Duration SAMPLING_PERIOD = Duration.ofMillis(20);

  private final IdGenerator idGenerator = IdGenerator.random();
  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final ScheduledExecutorStackTraceSampler sampler =
      new ScheduledExecutorStackTraceSampler(staging, SAMPLING_PERIOD);

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
    int expectedSamples = (int) HALF_SECOND.dividedBy(SAMPLING_PERIOD.multipliedBy(2));

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
    int expectedSamples = (int) HALF_SECOND.dividedBy(SAMPLING_PERIOD.multipliedBy(2));

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
    var latch = new CountDownLatch(1);
    var traceId = idGenerator.generateTraceId();
    var threadOne = startAndStopSampler(randomSpanContext(traceId), latch);
    var threadTwo = startAndStopSampler(randomSpanContext(traceId), latch);

    threadOne.start();
    threadTwo.start();
    await().atMost(HALF_SECOND).until(() -> staging.allStackTraces().size() > 5);
    latch.countDown();

    var threadIds =
        staging.allStackTraces().stream().map(StackTrace::getThreadId).collect(Collectors.toSet());
    assertThat(threadIds).containsOnly(threadOne.getId());
  }

  private Thread startAndStopSampler(SpanContext spanContext, CountDownLatch latch) {
    return new Thread(
        () -> {
          try {
            sampler.start(spanContext);
            latch.await();
            sampler.stop(spanContext);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void includeTimestampOnStackTraces() {
    var now = Instant.now();
    var spanContext = randomSpanContext();

    try {
      sampler.start(spanContext);
      await().atMost(HALF_SECOND).until(() -> !staging.allStackTraces().isEmpty());

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertThat(stackTrace.getTimestamp()).isNotNull().isAfter(now);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void includeDefaultSamplingPeriodOnFirstRecordedStackTraces() {
    var spanContext = randomSpanContext();

    try {
      sampler.start(spanContext);
      await().atMost(HALF_SECOND).until(() -> !staging.allStackTraces().isEmpty());

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertThat(stackTrace.getDuration()).isNotNull().isEqualTo(SAMPLING_PERIOD);
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void calculateSamplingPeriodAfterFirstRecordedStackTraces() {
    var spanContext = randomSpanContext();

    try {
      sampler.start(spanContext);
      await().until(() -> staging.allStackTraces().size() > 1);

      var stackTrace = staging.allStackTraces().stream().skip(1).findFirst().orElseThrow();
      assertThat(stackTrace.getDuration())
          .isNotNull()
          .isCloseTo(SAMPLING_PERIOD, Duration.ofMillis(5));
    } finally {
      sampler.stop(spanContext);
    }
  }

  @Test
  void includeTraceIdOnStackTraces() {
    var spanContext = randomSpanContext();

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
  void includeThreadDetailsOnStackTraces() {
    var traceId = idGenerator.generateTraceId();
    var spanContext = randomSpanContext(traceId);
    var latch = new CountDownLatch(1);
    try {
      var thread = startAndStopSampler(spanContext, latch);

      thread.start();
      await().atMost(HALF_SECOND).until(() -> !staging.allStackTraces().isEmpty());

      var stackTrace = staging.allStackTraces().stream().findFirst().orElseThrow();
      assertAll(
          () -> assertEquals(thread.getId(), stackTrace.getThreadId()),
          () -> assertEquals(thread.getName(), stackTrace.getThreadName()),
          () -> assertNotNull(stackTrace.getThreadState()),
          () -> assertThat(stackTrace.getStackFrames()).isNotEmpty());

      latch.countDown();
    } finally {
      sampler.stop(spanContext);
    }
  }

  private SpanContext randomSpanContext() {
    return randomSpanContext(idGenerator.generateTraceId());
  }

  private SpanContext randomSpanContext(String traceId) {
    return SpanContext.create(
        traceId, idGenerator.generateSpanId(), TraceFlags.getDefault(), TraceState.getDefault());
  }
}
