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
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.none;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaConstant;

public final class NocodeInstrumentation implements TypeInstrumentation {
  private final RuleImpl rule;

  public NocodeInstrumentation(RuleImpl rule) {
    this.rule = rule;
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // null rule is used when no rules are configured, this ensures that muzzle can collect helper
    // classes
    return rule != null ? rule.getClassMatcher() : none();
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        rule != null ? isMethod().and(rule.getMethodMatcher()) : none(),
        mapping ->
            mapping
                .bind(RuleId.class, JavaConstant.Simple.ofLoaded(rule != null ? rule.getId() : -1))
                .bind(
                    MethodReturnType.class,
                    (instrumentedType, instrumentedMethod, assigner, argumentHandler, sort) ->
                        Advice.OffsetMapping.Target.ForStackManipulation.of(
                            instrumentedMethod.getReturnType().asErasure())),
        this.getClass().getName() + "$NocodeAdvice");
  }

  // custom annotation that allows looking up the rule that triggered instrumenting the method
  @interface RuleId {}

  // custom annotation that represents the return type of the method
  @interface MethodReturnType {}

  @SuppressWarnings("unused")
  public static class NocodeAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @RuleId int ruleId,
        @Advice.Origin("#t") Class<?> declaringClass,
        @Advice.Origin("#m") String methodName,
        @Advice.Local("otelInvocation") NocodeMethodInvocation otelInvocation,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.This Object thiz,
        @Advice.AllArguments Object[] methodParams) {
      NocodeRules.Rule rule = NocodeRules.find(ruleId);
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
        @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue,
        @Advice.Thrown Throwable error) {
      if (scope == null) {
        return;
      }
      scope.close();

      returnValue =
          AsyncOperationEndSupport.create(instrumenter(), Object.class, method.getReturnType())
              .asyncEnd(context, otelInvocation, returnValue, error);
    }
  }
}
