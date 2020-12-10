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

package com.splunk.opentelemetry.middleware;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.MiddlewareHolder;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.jboss.as.version.ProductConfig;

@AutoService(InstrumentationModule.class)
public class WildFlyAttributesInstrumentationModule extends InstrumentationModule {

  public WildFlyAttributesInstrumentationModule() {
    super("wildfly");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("org.jboss.as.version.ProductConfig");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new Instrumentation());
  }

  public static class Instrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return named("org.jboss.as.server.ServerEnvironment");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return Collections.singletonMap(
          isConstructor(),
          WildFlyAttributesInstrumentationModule.class.getName() + "$MiddlewareInitializedAdvice");
    }
  }

  @SuppressWarnings("unused")
  public static class MiddlewareInitializedAdvice {
    @Advice.OnMethodEnter
    public static void onEnter() {
      CallDepthThreadLocalMap.incrementCallDepth(ProductConfig.class);
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.FieldValue("productConfig") ProductConfig productConfig) {
      if (CallDepthThreadLocalMap.decrementCallDepth(ProductConfig.class) == 0
          && productConfig != null) {
        MiddlewareHolder.trySetName(productConfig.resolveName());
        MiddlewareHolder.trySetVersion(productConfig.resolveVersion());
      }
    }
  }
}
