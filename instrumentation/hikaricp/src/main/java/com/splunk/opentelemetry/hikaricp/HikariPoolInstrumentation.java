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

package com.splunk.opentelemetry.hikaricp;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

class HikariPoolInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.zaxxer.hikari.pool.HikariPool");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("setMetricsTrackerFactory")
            .and(takesArguments(1))
            .and(takesArgument(0, named("com.zaxxer.hikari.metrics.MetricsTrackerFactory"))),
        this.getClass().getName() + "$SetMetricsTrackerFactoryAdvice");
  }

  @SuppressWarnings("unused")
  public static class SetMetricsTrackerFactoryAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(value = 0, readOnly = false) MetricsTrackerFactory metricsTrackerFactory) {
      if (!(metricsTrackerFactory instanceof MicrometerMetricsTrackerFactory)) {
        metricsTrackerFactory = new MicrometerMetricsTrackerFactory(metricsTrackerFactory);
      }
    }
  }
}
