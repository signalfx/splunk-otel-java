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

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.incubating.TelemetryIncubatingAttributes.TELEMETRY_DISTRO_VERSION;

import org.junit.jupiter.api.Test;

class SplunkDistroVersionResourceProviderTest {
  @Test
  void shouldGetDistroVersionFromProperties() {
    // given
    var provider = new SplunkDistroVersionResourceProvider();

    // when
    var resource = provider.createResource(null);

    // then
    assertThat(resource.getAttributes().size()).isEqualTo(1);
    assertThat(resource.getAttributes().get(TELEMETRY_DISTRO_VERSION)).isNotEmpty();
  }
}
