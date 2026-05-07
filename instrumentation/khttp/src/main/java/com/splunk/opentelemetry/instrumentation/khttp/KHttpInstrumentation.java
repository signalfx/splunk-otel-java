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

package com.splunk.opentelemetry.instrumentation.khttp;

import static com.splunk.opentelemetry.instrumentation.khttp.KHttpSingletons.instrumenter;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import khttp.responses.Response;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.AssignReturned;
import net.bytebuddy.asm.Advice.AssignReturned.ToArguments.ToArgument;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class KHttpInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("khttp.KHttp");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return extendsClass(named("khttp.KHttp"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(not(isAbstract()))
            .and(named("request"))
            .and(takesArgument(0, String.class))
            .and(takesArgument(1, String.class))
            .and(takesArgument(2, Map.class))
            .and(returns(named("khttp.responses.Response"))),
        KHttpInstrumentation.class.getName() + "$RequestAdvice");
  }

  @SuppressWarnings("unused")
  public static class RequestAdvice {

    public static class AdviceScope {
      private final RequestWrapper request;
      private final Context context;
      private final Scope scope;

      private AdviceScope(RequestWrapper request, Context context, Scope scope) {
        this.request = request;
        this.context = context;
        this.scope = scope;
      }

      @Nullable
      public static AdviceScope start(String method, String uri, Map<String, String> headers) {
        Context parentContext = Context.current();
        // Kotlin likes to use read-only data structures, so wrap into new writable map
        headers = new HashMap<>(headers);
        RequestWrapper request = new RequestWrapper(method, uri, headers);
        if (!instrumenter().shouldStart(parentContext, request)) {
          return null;
        }

        Context context = instrumenter().start(parentContext, request);
        return new AdviceScope(request, context, context.makeCurrent());
      }

      public void end(Response response, @Nullable Throwable throwable) {
        scope.close();
        instrumenter().end(context, request, response, throwable);
      }

      public Map<String, String> getHeaders() {
        return request.getHeaders();
      }
    }

    @AssignReturned.ToArguments(@ToArgument(value = 2, index = 1))
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Object[] methodEnter(
        @Advice.Argument(0) String method,
        @Advice.Argument(1) String uri,
        @Advice.Argument(2) Map<String, String> headers) {
      AdviceScope adviceScope = AdviceScope.start(method, uri, headers);
      if (adviceScope == null) {
        return new Object[] {null, headers};
      }

      return new Object[] {adviceScope, adviceScope.getHeaders()};
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return Response response,
        @Advice.Thrown Throwable throwable,
        @Advice.Enter Object[] enterResult) {
      AdviceScope adviceScope = (AdviceScope) enterResult[0];
      if (adviceScope != null) {
        adviceScope.end(response, throwable);
      }
    }
  }
}
