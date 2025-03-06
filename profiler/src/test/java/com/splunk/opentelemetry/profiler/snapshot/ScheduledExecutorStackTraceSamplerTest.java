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
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScheduledExecutorStackTraceSamplerTest {
  private static final Duration HALF_SECOND = Duration.ofMillis(500);
  private static final Duration PERIOD = Duration.ofMillis(20);

  private final IdGenerator idGenerator = IdGenerator.random();
  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final ScheduledExecutorStackTraceSampler sampler =
      new ScheduledExecutorStackTraceSampler(staging, PERIOD);

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
  void onlyTakeStackTraceSamplesForOneThreadPerTrace() throws Exception {
    var scheduler = Executors.newScheduledThreadPool(2);
    var latch = new CountDownLatch(1);
    var traceId = idGenerator.generateTraceId();

    var threadOne =
        scheduler.schedule(
            startSampling(randomSpanContext(traceId), latch), 0, TimeUnit.MILLISECONDS);
    scheduler.schedule(startSampling(randomSpanContext(traceId), latch), 25, TimeUnit.MILLISECONDS);

    await().atMost(HALF_SECOND).until(() -> staging.allStackTraces().size() > 5);
    latch.countDown();

    var threadIds =
        staging.allStackTraces().stream().map(StackTrace::getThreadId).collect(Collectors.toSet());
    assertThat(threadIds).containsOnly(threadOne.get().getId());
  }

  private Callable<Thread> startSampling(SpanContext spanContext, CountDownLatch latch) {
    return (() -> {
      try {
        System.out.println(
            "Starting thread "
                + Thread.currentThread().getName()
                + ", id "
                + Thread.currentThread().getId());
        sampler.start(spanContext);
        latch.await();
        return Thread.currentThread();
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
}
