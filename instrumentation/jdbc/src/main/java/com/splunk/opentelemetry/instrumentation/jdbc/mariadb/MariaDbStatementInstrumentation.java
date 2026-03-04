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

package com.splunk.opentelemetry.instrumentation.jdbc.mariadb;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.sql.Statement;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class MariaDbStatementInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // class names change between maridb driver versions
    return extendsClass(
        namedOneOf("org.mariadb.jdbc.Statement", "org.mariadb.jdbc.MariaDbStatement"));
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.mariadb.jdbc.Statement")
        .or(hasClassesNamed("org.mariadb.jdbc.MariaDbStatement"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isPublic()
            .and(
                nameStartsWith("execute")
                    .and(takesNoArguments().or(takesArgument(0, String.class)))),
        this.getClass().getName() + "$SetContextAdvice");
  }

  @SuppressWarnings("unused")
  public static class SetContextAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.This Statement statement, @Advice.Local("splunkCallDepth") CallDepth callDepth)
        throws Exception {
      callDepth = CallDepth.forClass(MariaDbContextPropagator.class);
      if (callDepth.getAndIncrement() > 0) {
        return;
      }

      MariaDbContextPropagator.INSTANCE.propagateContext(statement.getConnection());
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Local("splunkCallDepth") CallDepth callDepth) {
      callDepth.decrementAndGet();
    }
  }
}
