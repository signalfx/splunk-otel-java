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

package com.splunk.opentelemetry.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtelNamingConventionTest {
  @Mock NamingConvention namingConventionMock;

  NamingConvention otelNamingConvention;

  @BeforeEach
  void setUp() {
    otelNamingConvention = new OtelNamingConvention(namingConventionMock);
  }

  @Test
  void shouldPrependJvmMetricsWithRuntime() {
    // given
    var finalMeterName = "sf_runtime.jvm.test.meter";
    given(namingConventionMock.name("runtime.jvm.test.meter", Meter.Type.OTHER, "unit"))
        .willReturn(finalMeterName);

    // when
    var result = otelNamingConvention.name("jvm.test.meter", Meter.Type.OTHER, "unit");

    // then
    assertEquals(finalMeterName, result);
  }

  @Test
  void shouldNotModifyNonJvmMeterNames() {
    // given
    var finalMeterName = "sf_other.meter";
    given(namingConventionMock.name("other.meter", Meter.Type.OTHER, "unit"))
        .willReturn(finalMeterName);

    // when
    var result = otelNamingConvention.name("other.meter", Meter.Type.OTHER, "unit");

    // then
    assertEquals(finalMeterName, result);
  }
}
