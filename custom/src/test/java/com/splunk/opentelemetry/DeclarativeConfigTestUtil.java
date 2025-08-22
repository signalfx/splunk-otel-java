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

import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DeclarativeConfigTestUtil {
  private DeclarativeConfigTestUtil() {}

  public static OpenTelemetryConfigurationModel parseModel(String yaml) {
    try (InputStream yamlStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
      return DeclarativeConfiguration.parse(yamlStream);
    } catch (IOException e) {
      throw new RuntimeException("Could not parse YAML from string", e);
    }
  }

  public static OpenTelemetryConfigurationModel parseAndCustomizeModel(
      String yaml, DeclarativeConfigurationCustomizerProvider customizer) {
    OpenTelemetryConfigurationModel model = parseModel(yaml);
    DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
    customizer.customize(builder);

    builder.customizeModel(model);
    return model;
  }
}
