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

package com.splunk.opentelemetry.instrumentation.jdbc;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.incubator.config.internal.DeclarativeConfigUtil;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.internal.SqlCommenterBuilder;
import io.opentelemetry.javaagent.bootstrap.internal.sqlcommenter.SqlCommenterCustomizer;

@AutoService(SqlCommenterCustomizer.class)
public class SqlCommenterInitializer implements SqlCommenterCustomizer {
  // propagates service.name and other static attributes
  static TextMapPropagator propagator = TextMapPropagator.noop();

  @Override
  public void customize(SqlCommenterBuilder sqlCommenterBuilder) {
    sqlCommenterBuilder.setEnabled(
        DeclarativeConfigUtil.getInstrumentationConfig(GlobalOpenTelemetry.get(), "splunk-jdbc")
            .getBoolean("enabled", false));
    sqlCommenterBuilder.setPropagator(
        (connection, executed) -> {
          // note that besides jdbc this applies to r2dbc and other data access apis that upstream
          // has sqlcommenter support for
          return propagator;
        });
  }
}
