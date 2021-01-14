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

package com.splunk.opentelemetry.middleware.weblogic;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Adds an instrumentation to collect middleware attributes for WebLogic Server 12 and 14. As span
 * detection on WebLogic does not require any special logic, this does not initiate servlet spans by
 * itself, but saves the special attributes as a map to a servlet request attribute, which is then
 * later read when span is started by generic servlet instrumentation.
 */
@AutoService(InstrumentationModule.class)
public class WebLogicInstrumentationModule extends InstrumentationModule {
  public WebLogicInstrumentationModule() {
    super("weblogic");
  }

  @Override
  public int getOrder() {
    // Make sure this runs after default servlet instrumentations so that server span would already
    // be available.
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new MiddlewareInstrumentation(), new ServletInstrumentation());
  }

  private static class MiddlewareInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return named("weblogic.servlet.internal.WebAppServletContext$ServletInvocationAction");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          named("wrapRun")
              .and(takesArgument(1, named("javax.servlet.http.HttpServletRequest")))
              .and(isPrivate()),
          WebLogicInstrumentationModule.class.getName() + "$MiddlewareAdvice");
    }
  }

  private static class MiddlewareAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void start(@Advice.Argument(1) HttpServletRequest servletRequest) {
      WebLogicAttributeCollector.attachMiddlewareAttributes(servletRequest);
    }
  }

  private static class ServletInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
      return hasClassesNamed("javax.servlet.http.HttpServlet");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return extendsClass(named("javax.servlet.http.HttpServlet"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          named("service")
              .or(nameStartsWith("do")) // doGet, doPost, etc
              .and(takesArgument(0, named("javax.servlet.http.HttpServletRequest")))
              .and(takesArgument(1, named("javax.servlet.http.HttpServletResponse")))
              .and(isProtected().or(isPublic())),
          WebLogicInstrumentationModule.class.getName() + "$ServletAdvice");
    }
  }

  private static class ServletAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void start(
        @Advice.Argument(value = 0, readOnly = false) HttpServletRequest request) {
      Object value = request.getAttribute("otel.middleware");

      if (value instanceof Map<?, ?>) {
        Span span = Java8BytecodeBridge.currentSpan();

        if (span != null) {
          for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (entry.getValue() != null) {
              span.setAttribute(entry.getKey().toString(), entry.getValue().toString());
            }
          }
        }
      }
    }
  }

  @Override
  protected String[] additionalHelperClassNames() {
    String packageName = this.getClass().getPackage().getName();

    return new String[] {
      packageName + ".WebLogicAttributeCollector",
      packageName + ".WebLogicEntity",
      packageName + ".WebLogicEntity$Request",
      packageName + ".WebLogicEntity$Context",
      packageName + ".WebLogicEntity$Server",
      packageName + ".WebLogicEntity$Bean"
    };
  }
}
