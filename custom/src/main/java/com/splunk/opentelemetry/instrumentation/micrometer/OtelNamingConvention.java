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
  private final NamingConvention delegate;

  OtelNamingConvention(NamingConvention delegate) {
    this.delegate = delegate;
  }

  @Override
  public String name(String name, Meter.Type type, String baseUnit) {
    if (name.startsWith("jvm.")) {
      name = "runtime." + name;
    }
    return delegate.name(name, type, baseUnit);
  }

  @Override
  public String tagKey(String key) {
    return delegate.tagKey(key);
  }

  @Override
  public String tagValue(String value) {
    return delegate.tagValue(value);
  }
}
