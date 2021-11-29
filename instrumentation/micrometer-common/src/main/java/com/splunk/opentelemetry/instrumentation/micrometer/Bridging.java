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

package com.splunk.opentelemetry.instrumentation.micrometer;

import application.io.micrometer.core.instrument.Measurement;
import application.io.micrometer.core.instrument.Meter;
import application.io.micrometer.core.instrument.Statistic;
import application.io.micrometer.core.instrument.Tag;
import application.io.micrometer.core.instrument.distribution.CountAtBucket;
import application.io.micrometer.core.instrument.distribution.HistogramSnapshot;
import application.io.micrometer.core.instrument.distribution.ValueAtPercentile;
import application.io.micrometer.core.instrument.distribution.pause.ClockDriftPauseDetector;
import application.io.micrometer.core.instrument.distribution.pause.PauseDetector;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

public final class Bridging {

  @Nullable
  static io.micrometer.core.instrument.Meter.Id toAgent(@Nullable Meter.Id application) {
    if (application == null) {
      return null;
    }
    return new io.micrometer.core.instrument.Meter.Id(
        application.getName(),
        toAgentTags(application.getTagsAsIterable()),
        application.getBaseUnit(),
        application.getDescription(),
        toAgent(application.getType()));
  }

  static io.micrometer.core.instrument.Meter.Type toAgent(Meter.Type application) {
    return io.micrometer.core.instrument.Meter.Type.valueOf(application.name());
  }

  public static io.micrometer.core.instrument.Tags toAgentTags(Iterable<Tag> application) {
    return StreamSupport.stream(application.spliterator(), false)
        .map(Bridging::toAgent)
        .reduce(
            io.micrometer.core.instrument.Tags.empty(),
            io.micrometer.core.instrument.Tags::and,
            io.micrometer.core.instrument.Tags::concat);
  }

  static io.micrometer.core.instrument.Tag toAgent(Tag application) {
    return io.micrometer.core.instrument.Tag.of(application.getKey(), application.getValue());
  }

  static Iterable<io.micrometer.core.instrument.Measurement> toAgentMeasurements(
      Iterable<Measurement> application) {
    return StreamSupport.stream(application.spliterator(), false)
        .map(Bridging::toAgent)
        .collect(Collectors.toList());
  }

  static Iterable<Measurement> toApplicationMeasurements(
      Iterable<io.micrometer.core.instrument.Measurement> agent) {
    return StreamSupport.stream(agent.spliterator(), false)
        .map(Bridging::toApplication)
        .collect(Collectors.toList());
  }

  static io.micrometer.core.instrument.Measurement toAgent(Measurement application) {
    return new io.micrometer.core.instrument.Measurement(
        application::getValue, toAgent(application.getStatistic()));
  }

  static Measurement toApplication(io.micrometer.core.instrument.Measurement agent) {
    return new Measurement(agent::getValue, toApplication(agent.getStatistic()));
  }

  static io.micrometer.core.instrument.Statistic toAgent(Statistic application) {
    return io.micrometer.core.instrument.Statistic.valueOf(application.name());
  }

  static Statistic toApplication(io.micrometer.core.instrument.Statistic agent) {
    return Statistic.valueOf(agent.name());
  }

  static io.micrometer.core.instrument.distribution.pause.PauseDetector toAgent(
      PauseDetector applicationPauseDetector) {
    if (applicationPauseDetector instanceof ClockDriftPauseDetector) {
      ClockDriftPauseDetector clockDriftDetector =
          (ClockDriftPauseDetector) applicationPauseDetector;
      return new io.micrometer.core.instrument.distribution.pause.ClockDriftPauseDetector(
          clockDriftDetector.getSleepInterval(), clockDriftDetector.getPauseThreshold());
    }
    return new io.micrometer.core.instrument.distribution.pause.NoPauseDetector();
  }

  public static HistogramSnapshot toApplication(
      io.micrometer.core.instrument.distribution.HistogramSnapshot agent) {
    return new HistogramSnapshot(
        agent.count(),
        agent.total(),
        agent.max(),
        toApplication(agent.percentileValues()),
        toApplication(agent.histogramCounts()),
        agent::outputSummary);
  }

  @Nullable
  static ValueAtPercentile[] toApplication(
      @Nullable io.micrometer.core.instrument.distribution.ValueAtPercentile[] agent) {
    if (agent == null) {
      return null;
    }
    return Arrays.stream(agent)
        .map(v -> new ValueAtPercentile(v.percentile(), v.value()))
        .toArray(ValueAtPercentile[]::new);
  }

  @Nullable
  static CountAtBucket[] toApplication(
      @Nullable io.micrometer.core.instrument.distribution.CountAtBucket[] agent) {
    if (agent == null) {
      return null;
    }
    return Arrays.stream(agent)
        .map(v -> new CountAtBucket((long) v.bucket(), v.count()))
        .toArray(CountAtBucket[]::new);
  }

  @Nullable
  public static Duration[] toDurations(@Nullable double[] nanoTimes) {
    if (nanoTimes == null) {
      return null;
    }
    return Arrays.stream(nanoTimes)
        .mapToObj(d -> Duration.ofNanos((long) d))
        .toArray(Duration[]::new);
  }

  @Nullable
  public static Duration toDuration(@Nullable Double nanoTime) {
    return nanoTime == null ? null : Duration.ofNanos(nanoTime.longValue());
  }

  private Bridging() {}
}
