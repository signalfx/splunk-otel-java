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

package com.splunk.opentelemetry.instrumentation.jdbc.mysql;

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

public class MySqlStatementInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return namedOneOf(
        "com.mysql.cj.jdbc.StatementImpl", "com.mysql.cj.jdbc.ClientPreparedStatement");
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
    public static CallDepth onEnter(@Advice.This Statement statement) throws Exception {
      CallDepth callDepth = CallDepth.forClass(MySqlContextPropagator.class);
      if (callDepth.getAndIncrement() == 0) {
        MySqlContextPropagator.INSTANCE.propagateContext(statement.getConnection());
      }

      return callDepth;
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Enter CallDepth callDepth) {
      callDepth.decrementAndGet();
    }
  }
}
