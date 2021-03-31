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

package com.splunk.opentelemetry.testing;

import static java.util.stream.Collectors.toMap;

import io.opentelemetry.javaagent.testing.common.AgentClassLoaderAccess;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

public final class TestMetricsAccess {
  private static final MethodHandle getMeters;
  private static final MethodHandle clearMetrics;

  static {
    try {
      Class<?> testMetricsClass =
          AgentClassLoaderAccess.getAgentClassLoader()
              .loadClass("com.splunk.opentelemetry.testing.TestMetrics");
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      getMeters =
          lookup.findStatic(testMetricsClass, "getMeters", MethodType.methodType(Map.class));
      clearMetrics =
          lookup.findStatic(testMetricsClass, "clearMetrics", MethodType.methodType(void.class));
    } catch (Exception e) {
      throw new Error("Error accessing fields with reflection.", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, MeterData> getMeters() {
    try {
      Map<String, Map<String, Object>> rawMeters =
          (Map<String, Map<String, Object>>) getMeters.invokeExact();
      return rawMeters.entrySet().stream()
          .collect(toMap(Map.Entry::getKey, e -> MeterData.fromMap(e.getValue())));
    } catch (Throwable throwable) {
      throw new AssertionError("Could not invoke getMeterNames", throwable);
    }
  }

  public static void clearMetrics() {
    try {
      clearMetrics.invokeExact();
    } catch (Throwable throwable) {
      throw new AssertionError("Could not invoke clearMetrics", throwable);
    }
  }

  private TestMetricsAccess() {}
}
