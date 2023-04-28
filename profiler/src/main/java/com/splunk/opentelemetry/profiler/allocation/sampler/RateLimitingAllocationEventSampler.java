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

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.openjdk.jmc.common.item.IItem;

public class RateLimitingAllocationEventSampler implements AllocationEventSampler {
  private final String rateLimitString;
  private double maxEventsPerSecond;
  private ProbabilisticAllocationEventSampler delegate;

  public RateLimitingAllocationEventSampler(String rateLimitString) {
    this.rateLimitString = rateLimitString;
    maxEventsPerSecond = parseRateLimit(rateLimitString);
  }

  private static double parseRateLimit(String rateLimitString) {
    boolean inSecond = rateLimitString.endsWith("/s");
    boolean inMinute = rateLimitString.endsWith("/m");
    if (!inSecond && !inMinute) {
      throw getRateLimitException(rateLimitString, null);
    }
    String limit = rateLimitString.substring(0, rateLimitString.length() - 2);
    try {
      int value = Integer.parseInt(limit);
      return inMinute ? value / 60.0 : value;
    } catch (NumberFormatException exception) {
      throw getRateLimitException(rateLimitString, exception);
    }
  }

  private static IllegalArgumentException getRateLimitException(
      String rateLimitString, Throwable cause) {
    return new IllegalArgumentException(
        "Invalid rate limit '" + rateLimitString + "' valid rate limit is '100/s' or '10/m'",
        cause);
  }

  @Override
  public boolean shouldSample(IItem event) {
    if (delegate == null) {
      throw new IllegalStateException("delegate not set");
    }
    return delegate.shouldSample(event);
  }

  public void updateSampler(long eventCount, Instant periodStart, Instant periodEnd) {
    long period = Duration.between(periodStart, periodEnd).toMillis();
    double desiredEventsInPeriod = maxEventsPerSecond * period / TimeUnit.SECONDS.toMillis(1);
    double probability = clamp(desiredEventsInPeriod / eventCount, 0, 1);

    updateSampler(probability);
  }

  @VisibleForTesting
  public void updateSampler(double probability) {
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

  @VisibleForTesting
  public double maxEventsPerSecond() {
    return maxEventsPerSecond;
  }
}
