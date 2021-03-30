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

package com.splunk.opentelemetry.commonsdbcp2;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.commons.dbcp2.BasicDataSource;

@AutoService(InstrumentationModule.class)
public class CommonsDbcp2InstrumentationModule extends InstrumentationModule {
  public CommonsDbcp2InstrumentationModule() {
    super("commons-dbcp2");
  }

  @Override
  protected boolean defaultEnabled() {
    return Config.get().getBooleanProperty("splunk.metrics.enabled", true)
        && super.defaultEnabled();
  }

  @Override
  protected String[] additionalHelperClassNames() {
    return new String[] {
      "com.splunk.opentelemetry.commonsdbcp2.DataSourceMetrics",
      "com.splunk.opentelemetry.commonsdbcp2.DataSourceMetrics$TotalConnectionsUsed"
    };
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new BasicDataSourceInstrumentation());
  }

  private static class BasicDataSourceInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return named("org.apache.commons.dbcp2.BasicDataSource");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          isPublic().and(named("preRegister")).and(takesArguments(2)),
          this.getClass().getName() + "$PreRegisterAdvice");
    }

    public static class PreRegisterAdvice {
      @Advice.OnMethodExit(suppress = Throwable.class)
      public static void onExit(
          @Advice.This BasicDataSource dataSource, @Advice.Return ObjectName objectName) {
        DataSourceMetrics.registerMetrics(dataSource, objectName);
      }
    }
  }
}
