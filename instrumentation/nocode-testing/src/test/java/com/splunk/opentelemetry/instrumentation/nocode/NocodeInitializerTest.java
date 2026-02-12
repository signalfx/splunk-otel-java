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

package com.splunk.opentelemetry.instrumentation.nocode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class NocodeInitializerTest {
  @RegisterExtension final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();
  private List<NocodeRules.Rule> previousRules;

  @BeforeEach
  void captureRules() {
    previousRules = new ArrayList<>();
    NocodeRules.getGlobalRules().forEach(previousRules::add);
  }

  @AfterEach
  void restoreRules() {
    NocodeRules.setGlobalRules(previousRules);
  }

  @Test
  void shouldReadNoRulesIfNoFilePathProvided() {
    assertThat(NocodeInitializer.readRulesFromFile(null)).isEmpty();
    assertThat(NocodeInitializer.readRulesFromFile("   ")).isEmpty();
  }

  @Test
  void shouldSurviveMissingRulesFile() {
    assertThat(NocodeInitializer.readRulesFromFile("nonexistingFile.yaml")).isEmpty();
  }

  @Test
  void processRulesFromFile(@TempDir Path tempDir) throws IOException {
    // given
    String rulesYaml = "- class: foo.Foo\n  method: bar\n";
    Path rulesFilePath = writeRulesFile(tempDir, rulesYaml);

    AutoConfiguredOpenTelemetrySdk sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(
                () ->
                    Collections.singletonMap(
                        "splunk.otel.instrumentation.nocode.yml.file", rulesFilePath.toString()))
            .build();
    autoCleanup.deferCleanup(sdk.getOpenTelemetrySdk());

    // when
    new NocodeInitializer().beforeAgent(sdk);

    // then
    assertThat(NocodeRules.getGlobalRules().iterator().hasNext()).isTrue();
  }

  @Nested
  class DeclarativeConfigRules {
    @Test
    void shouldSurviveMissingInstrumentationConfigNode() {
      // given
      ConfigProvider provider = mock(ConfigProvider.class);
      when(provider.getInstrumentationConfig("splunk")).thenReturn(null);

      // when
      List<NocodeRules.Rule> rules = NocodeInitializer.readDeclarativeConfigRules(provider);

      // then
      assertThat(rules).isEmpty();
    }

    @Test
    void throwExceptionWhenRulesFileAndEmbeddedRulesAreDefined(@TempDir Path tempDir)
        throws IOException {
      // given
      String yaml =
          """
              file_format: "1.0-rc.3"
              instrumentation/development:
                java:
                  splunk:
                    no_code_file: "/tmp/does-not-exist.yml"
                    no_code:
                      - class: foo.Foo
                        method: bar
              """;

      AutoConfiguredOpenTelemetrySdk sdk =
          DeclarativeConfigTestUtil.createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

      // then
      assertThatThrownBy(() -> new NocodeInitializer().beforeAgent(sdk))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void processRulesFromRulesFile(@TempDir Path tempDir) throws IOException {
      // given
      Path rulesFile = writeRulesFile(tempDir, "- class: foo.Foo\n  method: bar\n");

      String yaml =
          """
              file_format: "1.0-rc.3"
              instrumentation/development:
                java:
                  splunk:
                    no_code_file: "%s"
              """
              .formatted(rulesFile.toString());

      AutoConfiguredOpenTelemetrySdk sdk =
          DeclarativeConfigTestUtil.createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

      // when
      new NocodeInitializer().beforeAgent(sdk);

      // then
      assertThat(NocodeRules.getGlobalRules().iterator().hasNext()).isTrue();
    }

    @Test
    void processRulesFromDeclarativeConfiguration(@TempDir Path tempDir) throws IOException {
      // given
      String yaml =
          """
              file_format: "1.0-rc.3"
              instrumentation/development:
                java:
                  splunk:
                    no_code:
                      - class: foo.Foo
                        method: bar
              """;

      AutoConfiguredOpenTelemetrySdk sdk =
          DeclarativeConfigTestUtil.createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

      // when
      new NocodeInitializer().beforeAgent(sdk);

      // then
      assertThat(NocodeRules.getGlobalRules().iterator().hasNext()).isTrue();
    }
  }

  private static Path writeRulesFile(Path tempDir, String yaml) throws IOException {
    Path file = tempDir.resolve("nocode.yml");
    Files.writeString(file, yaml);
    return file;
  }
}
