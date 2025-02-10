package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Method;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static com.splunk.opentelemetry.instrumentation.nocode.NocodeSingletons.instrumentor;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class NocodeInstrumentation implements TypeInstrumentation {
  private final NocodeRules.Rule rule;

  public NocodeInstrumentation(NocodeRules.Rule rule) {
    this.rule = rule;
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // exact match for now, if updated think about class name to use in naming the span/attributes
    return named(rule.className);
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(named(rule.methodName),
        NocodeInstrumentation.class.getName()+"$NocodeAdvice");
  }

  @SuppressWarnings("unused")
  public static class NocodeAdvice {
    @Advice.OnMethodEnter(suppress =  Throwable.class)
    public static void onEnter(
        @Advice.Origin("#t") Class<?> declaringClass,
        @Advice.Origin("#m") String methodName,
        @Advice.Local("otelInvocation") NocodeMethodInvocation otelInvocation,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.This Object thiz,
        @Advice.AllArguments Object[] methodParams
    ) {
      System.out.println("JBLEY INJECTED START");
      NocodeRules.Rule rule = NocodeRules.findRuleByClassAndMethod(declaringClass.getName(), methodName);
      otelInvocation = new NocodeMethodInvocation(
          rule,
          ClassAndMethod.create(declaringClass, methodName),
          thiz,
          methodParams);
      Context parentContext = currentContext();

      if (!instrumentor().shouldStart(parentContext, otelInvocation)) {
        return;
      }
      context = instrumentor().start(parentContext, otelInvocation);
      scope = context.makeCurrent();
    }

    public NocodeAdvice() {
      super();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Origin Method method,
        @Advice.Local("otelInvocation") NocodeMethodInvocation otelInvocation,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Thrown Throwable error)
    {
      System.out.println("JBLEY INJECTED END");
      if (scope == null) {
        return;
      }
      scope.close();
      // I wonder why the orignal methods instrumentation had support for modifying return value?
      instrumentor().end(context, otelInvocation, null, error);
    }
  }
}
