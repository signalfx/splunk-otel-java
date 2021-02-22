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

package com.splunk.opentelemetry.servlet;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class ServletInstrumentationModule extends InstrumentationModule {
  public ServletInstrumentationModule() {
    super("servlet");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("javax.servlet.Filter", "javax.servlet.http.HttpServlet");
  }

  protected String[] additionalHelperClassNames() {
    return new String[] {
      ServerTimingHeader.class.getName(), getClass().getName() + "$HeadersSetter"
    };
  }

  // run after the upstream servlet instrumentation - server span needs to be accessible here
  public int getOrder() {
    return 1;
  }

  // enable the instrumentation only if the server-timing header flag is on
  protected boolean defaultEnabled() {
    return super.defaultEnabled() && ServerTimingHeader.shouldEmitServerTimingHeader();
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new ServletAndFilterInstrumentation());
  }

  private static final class ServletAndFilterInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return safeHasSuperType(namedOneOf("javax.servlet.Filter", "javax.servlet.http.HttpServlet"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          namedOneOf("doFilter", "service")
              .and(takesArgument(0, named("javax.servlet.ServletRequest")))
              .and(takesArgument(1, named("javax.servlet.ServletResponse")))
              .and(isPublic()),
          getClass().getName() + "$AddHeadersAdvice");
    }

    public static class AddHeadersAdvice {
      @Advice.OnMethodEnter(suppress = Throwable.class)
      public static void onEnter(@Advice.Argument(1) ServletResponse response) {
        if (response instanceof HttpServletResponse) {
          HttpServletResponse httpResponse = (HttpServletResponse) response;
          Context context = Java8BytecodeBridge.currentContext();
          ServerTimingHeader.setHeaders(context, httpResponse, HeadersSetter.INSTANCE);
        }
      }
    }
  }

  public static final class HeadersSetter implements TextMapSetter<HttpServletResponse> {
    public static final HeadersSetter INSTANCE = new HeadersSetter();

    @Override
    public void set(HttpServletResponse carrier, String key, String value) {
      carrier.addHeader(key, value);
    }
  }
}
