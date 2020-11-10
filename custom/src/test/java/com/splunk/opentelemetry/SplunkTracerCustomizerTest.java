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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.then;

import io.opentelemetry.sdk.trace.TracerSdkProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SplunkTracerCustomizerTest {
  @Mock private TracerSdkProvider tracerSdkProvider;

  @Test
  public void shouldAddSpanProcessorsIfPropertiesAreSetToTrue() {

    // given
    SplunkTracerCustomizer underTest = new SplunkTracerCustomizer();
    System.setProperty(ENABLE_JDBC_SPAN_LOW_CARDINALITY_NAME_PROPERTY, "true");

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider).should().addSpanProcessor(isA(JdbcSpanRenamingProcessor.class));
    then(tracerSdkProvider).shouldHaveNoMoreInteractions();
  }

  @Test
  public void shouldNotAddSpanProcessorsIfPropertiesAreSetToAnythingElse() {

    // given
    SplunkTracerCustomizer underTest = new SplunkTracerCustomizer();
    System.setProperty(ENABLE_JDBC_SPAN_LOW_CARDINALITY_NAME_PROPERTY, "whatever");

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider).shouldHaveNoInteractions();
  }

  @Test
  public void shouldConfigureTracerSdkForDefaultValues() {

    // given
    SplunkTracerCustomizer underTest = new SplunkTracerCustomizer();
    System.clearProperty(ENABLE_JDBC_SPAN_LOW_CARDINALITY_NAME_PROPERTY);

    // when
    underTest.configure(tracerSdkProvider);

    // then
    then(tracerSdkProvider).should().addSpanProcessor(isA(JdbcSpanRenamingProcessor.class));
    then(tracerSdkProvider).shouldHaveNoMoreInteractions();
  }
}
