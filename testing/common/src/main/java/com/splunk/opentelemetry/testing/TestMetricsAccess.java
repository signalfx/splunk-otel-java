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

import io.opentelemetry.javaagent.testing.common.AgentClassLoaderAccess;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

public final class TestMetricsAccess {
  private static final MethodHandle getMeterNames;

  static {
    try {
      Class<?> testMetricsClass =
          AgentClassLoaderAccess.getAgentClassLoader()
              .loadClass("com.splunk.opentelemetry.testing.TestMetrics");
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      getMeterNames =
          lookup.findStatic(testMetricsClass, "getMeterNames", MethodType.methodType(Set.class));
    } catch (Exception e) {
      throw new Error("Error accessing fields with reflection.", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Set<String> getMeterNames() {
    try {
      return (Set<String>) getMeterNames.invokeExact();
    } catch (Throwable throwable) {
      throw new AssertionError("Could not invoke getMeterNames", throwable);
    }
  }

  private TestMetricsAccess() {}
}
