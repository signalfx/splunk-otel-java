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

import static com.splunk.opentelemetry.InstrumentationLibraryTracerCustomizer.PROPERTY_SPAN_PROCESSOR_INSTR_LIB_ENABLED;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import io.opentelemetry.sdk.trace.TracerSdkProvider;
import org.junit.jupiter.api.Test;

public class InstrumentationLibraryTracerCustomizerTest {

  @Test
  public void shouldAddSpanProcessorIfPropertySetToTrue() {

    // given
    TracerSdkProvider tracerSdkProvider = mock(TracerSdkProvider.class);
    InstrumentationLibraryTracerCustomizer underTest = new InstrumentationLibraryTracerCustomizer();
    System.setProperty(PROPERTY_SPAN_PROCESSOR_INSTR_LIB_ENABLED, "true");

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider)
        .should()
        .addSpanProcessor(isA(InstrumentationLibrarySpanProcessor.class));
  }

  @Test
  public void shouldNotAddSpanProcessorIfPropertySetToAnythingElse() {

    // given
    TracerSdkProvider tracerSdkProvider = mock(TracerSdkProvider.class);
    InstrumentationLibraryTracerCustomizer underTest = new InstrumentationLibraryTracerCustomizer();
    System.setProperty(PROPERTY_SPAN_PROCESSOR_INSTR_LIB_ENABLED, "enabled");

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider)
        .should(never())
        .addSpanProcessor(isA(InstrumentationLibrarySpanProcessor.class));
  }

  @Test
  public void shouldNotAddSpanProcessorIfPropertyNotSet() {

    // given
    TracerSdkProvider tracerSdkProvider = mock(TracerSdkProvider.class);
    InstrumentationLibraryTracerCustomizer underTest = new InstrumentationLibraryTracerCustomizer();
    System.clearProperty(PROPERTY_SPAN_PROCESSOR_INSTR_LIB_ENABLED);

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider)
        .should(never())
        .addSpanProcessor(isA(InstrumentationLibrarySpanProcessor.class));
  }
}
