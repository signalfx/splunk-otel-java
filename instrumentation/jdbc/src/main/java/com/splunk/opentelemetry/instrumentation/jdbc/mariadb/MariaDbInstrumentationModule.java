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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class MariaDbInstrumentationModule extends InstrumentationModule {

  public MariaDbInstrumentationModule() {
    super("splunk-jdbc", "splunk-jdbc-mariadb");
  }

  @Override
  public int order() {
    // run after jdbc instrumentation
    return 1;
  }

  @Override
  public boolean defaultEnabled() {
    return false;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new MariaDbStatementInstrumentation());
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.splunk.opentelemetry.instrumentation");
  }

  @Override
  public List<String> exposedClassNames() {
    // Statement instrumentation uses MariaDbContextPropagator class with CallDepth to skip
    // instrumenting the nested statement that propagates context to db. This class is loaded in the
    // isolated instrumentation class loader. We expose it to application class loader so that tests
    // can temporarily disable instrumentation with CallDepth to read the curent tracing context
    // from the db.
    return Boolean.getBoolean("jdbcTestMode")
        ? singletonList(
            "com.splunk.opentelemetry.instrumentation.jdbc.mariadb.MariaDbContextPropagator")
        : emptyList();
  }
}
