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

package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.instrumentation.MetricsDependentInstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

// metrics are still experimental and can't be enabled by default - that's why we need a separate
// InstrumentationModule for them
@AutoService(InstrumentationModule.class)
public class TomcatMetricsInstrumentationModule extends MetricsDependentInstrumentationModule {
  public TomcatMetricsInstrumentationModule() {
    super("tomcat", "tomcat-metrics");
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.splunk.opentelemetry.instrumentation");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new AbstractEndpointInstrumentation());
  }
}
