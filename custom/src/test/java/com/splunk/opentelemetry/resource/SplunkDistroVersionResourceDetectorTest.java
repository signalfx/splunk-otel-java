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

package com.splunk.opentelemetry.resource;

import static io.opentelemetry.semconv.incubating.TelemetryIncubatingAttributes.TELEMETRY_DISTRO_VERSION;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.resources.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class SplunkDistroVersionResourceDetectorTest {
  @Test
  void shouldCreateResourceWithDistroVersionInformation() throws IOException {
    var properties = "telemetry.distro.version = 5.1.4";
    InputStream propertiesStream = new ByteArrayInputStream(properties.getBytes());

    Resource resource = SplunkDistroVersionResourceDetector.createResource(propertiesStream);

    assertThat(resource).isNotNull();
    assertThat(resource.getAttributes().asMap())
        .containsOnly(entry(TELEMETRY_DISTRO_VERSION, "5.1.4"));
  }
}
