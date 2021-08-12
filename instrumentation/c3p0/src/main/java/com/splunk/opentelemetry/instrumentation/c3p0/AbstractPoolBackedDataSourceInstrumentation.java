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

package com.splunk.opentelemetry.instrumentation.c3p0;

import static net.bytebuddy.matcher.ElementMatchers.named;

import com.mchange.v2.c3p0.impl.AbstractPoolBackedDataSource;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

final class AbstractPoolBackedDataSourceInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.mchange.v2.c3p0.impl.AbstractPoolBackedDataSource");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("resetPoolManager"), this.getClass().getName() + "$ResetPoolManagerAdvice");
    transformer.applyAdviceToMethod(named("close"), this.getClass().getName() + "$CloseAdvice");
  }

  @SuppressWarnings("unused")
  public static class ResetPoolManagerAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This AbstractPoolBackedDataSource dataSource) {
      ConnectionPoolMetrics.registerMetrics(dataSource);
    }
  }

  @SuppressWarnings("unused")
  public static class CloseAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(@Advice.This AbstractPoolBackedDataSource dataSource) {
      ConnectionPoolMetrics.unregisterMetrics(dataSource);
    }
  }
}
