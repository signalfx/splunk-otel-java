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

import static com.splunk.opentelemetry.SplunkTracerCustomizer.ENABLE_JDBC_SPAN_LOW_CARDINALITY_NAME_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SplunkTracerCustomizerTest {
  @Mock private TracerSdkProvider tracerSdkProvider;

  @Captor private ArgumentCaptor<TraceConfig> traceConfigCaptor;

  @Test
  public void shouldAddSpanProcessorsIfPropertiesAreSetToTrue() {

    // given
    SplunkTracerCustomizer underTest = new SplunkTracerCustomizer();
    System.setProperty(ENABLE_JDBC_SPAN_LOW_CARDINALITY_NAME_PROPERTY, "true");

    TraceConfig traceConfig = TraceConfig.getDefault();
    given(tracerSdkProvider.getActiveTraceConfig()).willReturn(traceConfig);

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider).should().addSpanProcessor(isA(JdbcSpanRenamingProcessor.class));
    then(tracerSdkProvider).should().updateActiveTraceConfig(traceConfigCaptor.capture());
    then(tracerSdkProvider).shouldHaveNoMoreInteractions();

    assertEquals(Sampler.alwaysOn(), traceConfigCaptor.getValue().getSampler());
  }

  @Test
  public void shouldNotAddSpanProcessorsIfPropertiesAreSetToAnythingElse() {

    // given
    SplunkTracerCustomizer underTest = new SplunkTracerCustomizer();
    System.setProperty(ENABLE_JDBC_SPAN_LOW_CARDINALITY_NAME_PROPERTY, "whatever");

    TraceConfig traceConfig = TraceConfig.getDefault();
    given(tracerSdkProvider.getActiveTraceConfig()).willReturn(traceConfig);

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider).should().updateActiveTraceConfig(traceConfigCaptor.capture());
    then(tracerSdkProvider).shouldHaveNoMoreInteractions();

    assertEquals(Sampler.alwaysOn(), traceConfigCaptor.getValue().getSampler());
  }

  @Test
  public void shouldConfigureTracerSdkForDefaultValues() {
    // given
    SplunkTracerCustomizer underTest = new SplunkTracerCustomizer();
    System.clearProperty(ENABLE_JDBC_SPAN_LOW_CARDINALITY_NAME_PROPERTY);

    TraceConfig traceConfig = TraceConfig.getDefault();
    given(tracerSdkProvider.getActiveTraceConfig()).willReturn(traceConfig);

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider).should().addSpanProcessor(isA(JdbcSpanRenamingProcessor.class));
    then(tracerSdkProvider).should().updateActiveTraceConfig(traceConfigCaptor.capture());
    then(tracerSdkProvider).shouldHaveNoMoreInteractions();

    assertEquals(Sampler.alwaysOn(), traceConfigCaptor.getValue().getSampler());
  }
}
