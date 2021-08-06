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

package com.splunk.opentelemetry.micrometer;

import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.testing.MeterData;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AgentInstrumentationExtension.class)
public class MicrometerBridgeTest {

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void testCounter() {
    // given
    var counter =
        Counter.builder("testCounter")
            .tags("tag", "value")
            .baseUnit("items")
            .register(Metrics.globalRegistry);

    // when
    counter.increment();
    counter.increment(2);

    // then
    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData("testCounter", "counter", "items", Map.of("tag", "value")));

    // assert both application and agent measurements
    assertThat(counter.count()).isEqualTo(3);
    assertThat(TestMetricsAccess.getMeasurements("testCounter")).containsExactly(3);

    // when
    Metrics.globalRegistry.remove(counter);

    // then
    assertThat(TestMetricsAccess.getMeterNames()).doesNotContain("testCounter");
  }

  @Test
  void testDistributionSummary() {
    // given
    var distributionSummary =
        DistributionSummary.builder("testDistributionSummary")
            .tag("tag", "value")
            .baseUnit("items")
            .register(Metrics.globalRegistry);

    // when
    distributionSummary.record(1);
    distributionSummary.record(2);
    distributionSummary.record(3);
    distributionSummary.record(4);

    // then
    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData(
                "testDistributionSummary",
                "distribution_summary",
                "items",
                Map.of("tag", "value")));

    // assert both application and agent measurements
    assertThat(distributionSummary.count()).isEqualTo(4);
    assertThat(distributionSummary.totalAmount()).isEqualTo(10);
    assertThat(TestMetricsAccess.getMeasurements("testDistributionSummary")).containsExactly(4, 10);

    // when
    Metrics.globalRegistry.remove(distributionSummary);

    // then
    assertThat(TestMetricsAccess.getMeterNames()).doesNotContain("testDistributionSummary");
  }

  @Test
  void testFunctionCounter() {
    // given
    var number = new AtomicInteger(42);
    var counter =
        FunctionCounter.builder("testFunctionCounter", number, AtomicInteger::get)
            .tags("tag", "value")
            .baseUnit("items")
            .register(Metrics.globalRegistry);

    // then
    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData("testFunctionCounter", "counter", "items", Map.of("tag", "value")));

    // assert both application and agent measurements
    assertThat(counter.count()).isEqualTo(42);
    assertThat(TestMetricsAccess.getMeasurements("testFunctionCounter")).containsExactly(42);

    // when
    Metrics.globalRegistry.remove(counter);

    // then
    assertThat(TestMetricsAccess.getMeterNames()).doesNotContain("testFunctionCounter");
  }

  @Test
  void testFunctionTimer() {
    // given
    class TestTimedType {
      long count() {
        return 42;
      }

      double totalTime() {
        return 12;
      }
    }
    var timedObj = new TestTimedType();

    // when
    var timer =
        FunctionTimer.builder(
                "testFunctionTimer",
                timedObj,
                TestTimedType::count,
                TestTimedType::totalTime,
                TimeUnit.SECONDS)
            .tags("tag", "value")
            .register(Metrics.globalRegistry);

    // then
    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData("testFunctionTimer", "timer", "seconds", Map.of("tag", "value")));

    // assert both application and agent measurements
    assertThat(timer.count()).isEqualTo(42);
    assertThat(timer.totalTime(TimeUnit.SECONDS)).isEqualTo(12);
    assertThat(TestMetricsAccess.getMeasurements("testFunctionTimer")).containsExactly(42, 12);

    // when
    Metrics.globalRegistry.remove(timer);

    // then
    assertThat(TestMetricsAccess.getMeterNames()).doesNotContain("testFunctionTimer");
  }

  @Test
  void testGauge() {
    // when
    var gauge =
        Gauge.builder("testGauge", () -> 42)
            .tags("tag", "value")
            .baseUnit("items")
            .register(Metrics.globalRegistry);

    // then
    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData("testGauge", "gauge", "items", Map.of("tag", "value")));

    // assert both application and agent measurements
    assertThat(gauge.value()).isEqualTo(42);
    assertThat(TestMetricsAccess.getMeasurements("testGauge")).containsExactly(42);

    // when
    Metrics.globalRegistry.remove(gauge);

    // then
    assertThat(TestMetricsAccess.getMeterNames()).doesNotContain("testGauge");
  }

  @Test
  void testLongTaskTimer() throws InterruptedException {
    // given
    var timer =
        LongTaskTimer.builder("testLongTaskTimer")
            .tags("tag", "value")
            .register(Metrics.globalRegistry);

    // when
    var sample = timer.start();
    Thread.sleep(50);

    // then
    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData(
                "testLongTaskTimer", "long_task_timer", "seconds", Map.of("tag", "value")));

    // assert both application and agent measurements
    assertThat(timer.activeTasks()).isEqualTo(1);
    assertThat(timer.duration(TimeUnit.NANOSECONDS))
        .isGreaterThan(TimeUnit.MILLISECONDS.toNanos(50));

    double[] agentMeasurements = TestMetricsAccess.getMeasurements("testLongTaskTimer");
    assertThat(agentMeasurements).hasSize(2);
    assertThat(agentMeasurements[0]).isEqualTo(1);
    assertThat(agentMeasurements[1]).isGreaterThan(TimeUnit.MILLISECONDS.toNanos(50));

    // then
    sample.stop();
    Metrics.globalRegistry.remove(timer);

    // then
    assertThat(TestMetricsAccess.getMeterNames()).doesNotContain("testLongTaskTimer");
  }

  @Test
  void testTimer() {
    // given
    var timer = Timer.builder("testTimer").tags("tag", "value").register(Metrics.globalRegistry);

    // when
    timer.record(12, TimeUnit.SECONDS);
    timer.record(42, TimeUnit.SECONDS);
    timer.record(10, TimeUnit.SECONDS);

    // then
    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData("testTimer", "timer", "seconds", Map.of("tag", "value")));

    // assert both application and agent measurements
    assertThat(timer.count()).isEqualTo(3);
    assertThat(timer.totalTime(TimeUnit.SECONDS)).isEqualTo(64);
    assertThat(timer.max(TimeUnit.SECONDS)).isEqualTo(42);

    assertThat(TestMetricsAccess.getMeasurements("testTimer")).containsExactly(3, 64, 42);

    // when
    Metrics.globalRegistry.remove(timer);

    // then
    assertThat(TestMetricsAccess.getMeterNames()).doesNotContain("testTimer");
  }
}
