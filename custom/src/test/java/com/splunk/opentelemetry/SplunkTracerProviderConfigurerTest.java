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

import static com.splunk.opentelemetry.SplunkTracerProviderConfigurer.SPLUNK_DEFAULT_ATTRIBUTE_VALUE_LENGTH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import org.junit.jupiter.api.Test;

class SplunkTracerProviderConfigurerTest {

  @Test
  void shouldConfigureSpanLimits() {
    // given
    var tracerProviderBuilder = mock(SdkTracerProviderBuilder.class);
    var config = mock(ConfigProperties.class);
    var underTest = new SplunkTracerProviderConfigurer();

    // when
    underTest.configure(tracerProviderBuilder, config);

    // then
    verify(tracerProviderBuilder)
        .setSpanLimits(
            SpanLimits.builder()
                .setMaxNumberOfAttributes(Integer.MAX_VALUE)
                .setMaxNumberOfEvents(Integer.MAX_VALUE)
                .setMaxNumberOfLinks(1000)
                .setMaxNumberOfAttributesPerEvent(Integer.MAX_VALUE)
                .setMaxNumberOfAttributesPerLink(Integer.MAX_VALUE)
                .setMaxAttributeValueLength(SPLUNK_DEFAULT_ATTRIBUTE_VALUE_LENGTH)
                .build());
  }
}
