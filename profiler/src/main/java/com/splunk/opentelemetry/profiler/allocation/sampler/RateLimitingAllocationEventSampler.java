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

package com.splunk.opentelemetry.profiler.allocation.sampler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import jdk.jfr.consumer.RecordedEvent;

public class RateLimitingAllocationEventSampler implements AllocationEventSampler {
  private final String rateLimitString;
  private int maxEventsPerSecond;
  private ProbabilisticAllocationEventSampler delegate;

  public RateLimitingAllocationEventSampler(String rateLimitString) {
    this.rateLimitString = rateLimitString;
    maxEventsPerSecond = parseRateLimit(rateLimitString);
  }

  private static int parseRateLimit(String rateLimitString) {
    if (!rateLimitString.endsWith("/s")) {
      throw new IllegalArgumentException("Invalid rate limit '" + rateLimitString + "'");
    }
    String limit = rateLimitString.substring(0, rateLimitString.length() - 2);
    return Integer.parseInt(limit);
  }

  @Override
  public boolean shouldSample(RecordedEvent event) {
    if (delegate == null) {
      throw new IllegalStateException("delegate not set");
    }
    return delegate.shouldSample(event);
  }

  public void updateSampler(long eventCount, Instant periodStart, Instant periodEnd) {
    long period = Duration.between(periodStart, periodEnd).toMillis();
    double desiredEventsInPeriod =
        (double) maxEventsPerSecond * period / TimeUnit.SECONDS.toMillis(1);
    double probability = clamp(desiredEventsInPeriod / eventCount, 0, 1);

    delegate = new ProbabilisticAllocationEventSampler(probability);
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  @Override
  public void addAttributes(
      BiConsumer<String, String> stringAttributeAdder,
      BiConsumer<String, Long> longAttributeAdder) {
    stringAttributeAdder.accept("sampler.name", "Rate limiting sampler");
    stringAttributeAdder.accept("sampler.limit", rateLimitString);
  }
}
