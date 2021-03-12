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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;

/**
 * This class adds {@code runtime.} prefix to all JVM metrics produced by micrometer (they all begin
 * with {@code jvm.} by default). There are two reasons to do that:
 *
 * <ol>
 *   <li>To match what OTel metrics spec says about runtime metrics: and pretty much the only thing
 *       that's specified there is that runtime metrics should start with {@code
 *       runtime.{environment}.}
 *   <li>To avoid conflicts with the {@code opentelemetry-java-contrib/jmx-metrics} tool that starts
 *       all metrics with {@code jvm.}
 * </ol>
 */
class OtelNamingConvention implements NamingConvention {
  private final NamingConvention signalfxNamingConvention;

  OtelNamingConvention(NamingConvention signalfxNamingConvention) {
    this.signalfxNamingConvention = signalfxNamingConvention;
  }

  @Override
  public String name(String name, Meter.Type type, String baseUnit) {
    String metricName = signalfxNamingConvention.name(name, type, baseUnit);
    return metricName.startsWith("jvm.") ? "runtime." + metricName : metricName;
  }

  @Override
  public String tagKey(String key) {
    return signalfxNamingConvention.tagKey(key);
  }

  @Override
  public String tagValue(String value) {
    return signalfxNamingConvention.tagValue(value);
  }
}
