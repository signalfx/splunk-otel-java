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

package com.splunk.opentelemetry.instrumentation.micrometer.v1_3;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.instrumentation.MetricsInstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class MicrometerInstrumentationModule extends MetricsInstrumentationModule {

  public MicrometerInstrumentationModule() {
    super("micrometer", "micrometer-1.3");
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.splunk.opentelemetry.instrumentation");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return not(
        hasClassesNamed("application.io.micrometer.core.instrument.config.validate.Validated"));
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new MetricsInstrumentation());
  }
}
