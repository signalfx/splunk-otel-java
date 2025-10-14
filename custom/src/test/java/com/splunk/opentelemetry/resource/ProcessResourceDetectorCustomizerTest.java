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

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.parseAndCustomizeModel;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import org.junit.jupiter.api.Test;

class ProcessResourceDetectorCustomizerTest {
  @Test
  void shouldAddTruncatingResourceDetectorWhenNoResourceDefinedInConfig() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
            """;
    ProcessResourceDetectorCustomizer customizer = new ProcessResourceDetectorCustomizer();

    // when
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    // then
    assertThat(model.getResource()).isNotNull();
    assertThat(model.getResource().getDetectionDevelopment()).isNotNull();
    assertThat(model.getResource().getDetectionDevelopment().getDetectors()).hasSize(1);

    ExperimentalResourceDetectorModel detectionModel =
        model.getResource().getDetectionDevelopment().getDetectors().get(0);
    assertThat(detectionModel.getAdditionalProperties()).isNotNull();
    assertThat(detectionModel.getAdditionalProperties())
        .containsKey(TruncateCommandLineResourceDetector.PROVIDER_NAME);
  }

  @Test
  void shouldAddTruncatingResourceDetectorToExistingDetectors() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            resource:
              detection/development:
                detectors:
                  - splunk_distro_version:
            instrumentation/development:
              java:
            """;
    ProcessResourceDetectorCustomizer customizer = new ProcessResourceDetectorCustomizer();

    // when
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    // then
    assertThat(model.getResource()).isNotNull();
    assertThat(model.getResource().getDetectionDevelopment()).isNotNull();
    assertThat(model.getResource().getDetectionDevelopment().getDetectors()).hasSize(2);

    ExperimentalResourceDetectorModel detectionModel =
        model.getResource().getDetectionDevelopment().getDetectors().get(1);
    assertThat(detectionModel.getAdditionalProperties()).isNotNull();
    assertThat(detectionModel.getAdditionalProperties())
        .containsKey(TruncateCommandLineResourceDetector.PROVIDER_NAME);
  }

  @Test
  void shouldFallBackToStandardProcessResourceDetectorWhenTruncateIsOff() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
                splunk:
                  metrics:
                    force_full_commandline: true
        """;
    ProcessResourceDetectorCustomizer customizer = new ProcessResourceDetectorCustomizer();

    // when
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    // then
    assertThat(model.getResource()).isNotNull();
    assertThat(model.getResource().getDetectionDevelopment()).isNotNull();
    assertThat(model.getResource().getDetectionDevelopment().getDetectors()).hasSize(1);

    ExperimentalResourceDetectorModel detectionModel =
        model.getResource().getDetectionDevelopment().getDetectors().get(0);
    assertThat(detectionModel.getAdditionalProperties()).isNotNull();
    assertThat(detectionModel.getAdditionalProperties()).containsKey("process");
  }
}
