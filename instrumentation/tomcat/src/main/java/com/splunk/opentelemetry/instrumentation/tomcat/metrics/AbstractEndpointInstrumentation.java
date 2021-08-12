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

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.tomcat.util.net.AbstractEndpoint;

final class AbstractEndpointInstrumentation implements TypeInstrumentation {

  // AbstractEndpoint encapsulates socket and worker pool management - the thread pools that you
  // configure in server.xml inside the <Connector/> tag are managed by AbstractEndpoint
  // tomcat also exposes AbstractEndpoint objects in JMX with type=ThreadPool
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.tomcat.util.net.AbstractEndpoint");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("init").and(takesArguments(0)), this.getClass().getName() + "$InitAdvice");
    transformer.applyAdviceToMethod(
        named("destroy").and(takesArguments(0)), this.getClass().getName() + "$DestroyAdvice");
  }

  @SuppressWarnings("unused")
  public static class InitAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This AbstractEndpoint<?, ?> endpoint) {
      ThreadPoolMetrics.registerMetrics(endpoint);
    }
  }

  @SuppressWarnings("unused")
  public static class DestroyAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(@Advice.This AbstractEndpoint<?, ?> endpoint) {
      ThreadPoolMetrics.unregisterMetrics(endpoint);
    }
  }
}
