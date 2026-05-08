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
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.none;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import javax.annotation.Nullable;
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
    public static class AdviceScope {
      private final NocodeMethodInvocation otelInvocation;
      private final Context context;
      private final Scope scope;

      private AdviceScope(NocodeMethodInvocation otelInvocation, Context context, Scope scope) {
        this.otelInvocation = otelInvocation;
        this.context = context;
        this.scope = scope;
      }

      @Nullable
      public static AdviceScope start(
          int ruleId,
          Class<?> declaringClass,
          String methodName,
          Object thiz,
          Object[] methodParams) {
        NocodeRules.Rule rule = NocodeRules.find(ruleId);
        NocodeMethodInvocation otelInvocation =
            new NocodeMethodInvocation(
                rule, ClassAndMethod.create(declaringClass, methodName), thiz, methodParams);

        Context parentContext = Context.current();
        if (rule != null && rule.isCurrentSpan()) {
          Span currentSpan = Span.fromContext(parentContext);
          if (currentSpan.isRecording()) {
            NocodeAttributesExtractor.applyToSpan(currentSpan, otelInvocation);
          }
          return null;
        }

        if (!instrumenter().shouldStart(parentContext, otelInvocation)) {
          return null;
        }
        Context context = instrumenter().start(parentContext, otelInvocation);
        return new AdviceScope(otelInvocation, context, context.makeCurrent());
      }

      public Object end(
          Class<?> methodReturnType, Object returnValue, @Nullable Throwable throwable) {
        scope.close();
        return AsyncOperationEndSupport.create(instrumenter(), Object.class, methodReturnType)
            .asyncEnd(context, otelInvocation, returnValue, throwable);
      }
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    @Nullable
    public static AdviceScope onEnter(
        @RuleId int ruleId,
        @Advice.Origin("#t") Class<?> declaringClass,
        @Advice.Origin("#m") String methodName,
        @Advice.This Object thiz,
        @Advice.AllArguments Object[] methodParams) {
      return AdviceScope.start(ruleId, declaringClass, methodName, thiz, methodParams);
    }

    @Advice.AssignReturned.ToReturned
    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static Object stopSpan(
        @MethodReturnType Class<?> methodReturnType,
        @Advice.Enter @Nullable AdviceScope adviceScope,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue,
        @Advice.Thrown Throwable error) {
      if (adviceScope != null) {
        return adviceScope.end(methodReturnType, returnValue, error);
      }
      return returnValue;
    }
  }
}
