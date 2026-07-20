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

package com.splunk.opentelemetry.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetadataGeneratorTest {

  @Test
  @SuppressWarnings("unchecked")
  void supportsReferencedDefinitionsInFileFormat06() throws IOException {
    List<Map<String, Object>> instrumentations =
        parseInstrumentations("instrumentation-list-0.6.yaml");

    assertThat(instrumentations)
        .singleElement()
        .satisfies(
            instrumentation -> {
              List<Map<String, Object>> settings =
                  (List<Map<String, Object>>) instrumentation.get("settings");
              assertThat(settings)
                  .extracting(setting -> setting.get("property"))
                  .containsExactly("otel.instrumentation.test.enabled");

              List<Map<String, Object>> signals =
                  (List<Map<String, Object>>) instrumentation.get("signals");
              List<Map<String, Object>> metrics =
                  (List<Map<String, Object>>) signals.get(0).get("metrics");
              assertThat(metrics)
                  .extracting(metric -> metric.get("metric_name"))
                  .containsExactly("test.requests");
            });
  }

  private static List<Map<String, Object>> parseInstrumentations(String resourceName)
      throws IOException {
    URL resource = MetadataGeneratorTest.class.getResource("/" + resourceName);
    assertThat(resource).isNotNull();
    return MetadataGenerator.parseInstrumentations(resource);
  }
}
