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

package com.splunk.opentelemetry;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.javaagent.bootstrap.spi.TracerCustomizer;
import io.opentelemetry.sdk.trace.TracerSdkProvider;

public class InstrumentationLibraryTracerCustomizer implements TracerCustomizer {

  @VisibleForTesting
  static final String PROPERTY_SPAN_PROCESSOR_INSTR_LIB_ENABLED =
      "splunk.otel.config.span.processor.instrlib.enabled";

  private static String propertyToEnv(String property) {
    return property.replace(".", "_").toUpperCase();
  }

  private static boolean spanProcessorInstrumentationLibraryEnabled() {
    String value = System.getProperty(PROPERTY_SPAN_PROCESSOR_INSTR_LIB_ENABLED);
    if (value == null) {
      value = System.getenv(propertyToEnv(PROPERTY_SPAN_PROCESSOR_INSTR_LIB_ENABLED));
    }
    return ("true".equalsIgnoreCase(value));
  }

  @Override
  public void configure(TracerSdkProvider tracerSdkProvider) {

    if (spanProcessorInstrumentationLibraryEnabled()) {
      tracerSdkProvider.addSpanProcessor(new InstrumentationLibrarySpanProcessor());
    }
  }
}
