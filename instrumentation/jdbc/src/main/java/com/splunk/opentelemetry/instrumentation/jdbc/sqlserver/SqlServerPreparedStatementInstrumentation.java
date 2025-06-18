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

package com.splunk.opentelemetry.instrumentation.jdbc.sqlserver;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.sql.Statement;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SqlServerPreparedStatementInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isPublic().and(nameStartsWith("execute").and(takesNoArguments())),
        this.getClass().getName() + "$SetContextAdvice");
  }

  @SuppressWarnings("unused")
  public static class SetContextAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.This Statement statement, @Advice.Local("splunkCallDepth") CallDepth callDepth)
        throws Exception {
      callDepth = CallDepth.forClass(SqlServerUtil.class);
      if (callDepth.getAndIncrement() > 0) {
        return;
      }

      SqlServerUtil.propagateContext(statement.getConnection());
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Local("splunkCallDepth") CallDepth callDepth) {
      callDepth.decrementAndGet();
    }
  }
}
