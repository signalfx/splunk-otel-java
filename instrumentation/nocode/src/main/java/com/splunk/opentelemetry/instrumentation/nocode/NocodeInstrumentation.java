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

package com.splunk.opentelemetry.instrumentation.nocode;

import static com.splunk.opentelemetry.instrumentation.nocode.NocodeSingletons.instrumenter;
import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public final class NocodeInstrumentation implements TypeInstrumentation {
  private final NocodeRules.Rule rule;

  public NocodeInstrumentation(NocodeRules.Rule rule) {
    this.rule = rule;
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // names have to match exactly for now to enable rule lookup
    // at advice time.  In the future, we could support
    // more complex rules here if we dynamically generated advice classes for
    // each rule.
    return rule != null ? named(rule.className) : none();
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        rule != null ? named(rule.methodName) : none(),
        this.getClass().getName() + "$NocodeAdvice");
  }

  @SuppressWarnings("unused")
  public static class NocodeAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin("#t") Class<?> declaringClass,
        @Advice.Origin("#m") String methodName,
        @Advice.Local("otelInvocation") NocodeMethodInvocation otelInvocation,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.This Object thiz,
        @Advice.AllArguments Object[] methodParams) {
      NocodeRules.Rule rule = NocodeRules.find(declaringClass.getName(), methodName);
      otelInvocation =
          new NocodeMethodInvocation(
              rule, ClassAndMethod.create(declaringClass, methodName), thiz, methodParams);
      Context parentContext = currentContext();

      if (!instrumenter().shouldStart(parentContext, otelInvocation)) {
        return;
      }
      context = instrumenter().start(parentContext, otelInvocation);
      scope = context.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Origin Method method,
        @Advice.Local("otelInvocation") NocodeMethodInvocation otelInvocation,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Thrown Throwable error) {
      if (scope == null) {
        return;
      }
      scope.close();
      // This is heavily based on the "methods" instrumentation from upstream, but
      // for now we're not supporting modifying return types/async result codes, etc.
      // This could be expanded in the future.
      instrumenter().end(context, otelInvocation, null, error);
    }
  }
}
