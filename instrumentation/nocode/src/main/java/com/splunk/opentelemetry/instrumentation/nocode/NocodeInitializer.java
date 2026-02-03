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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfigProvider;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.isDeclarativeConfig;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(BeforeAgentListener.class)
public class NocodeInitializer implements BeforeAgentListener {
  private static final String NOCODE_YMLFILE = "splunk.otel.instrumentation.nocode.yml.file";
  private static final Logger logger = Logger.getLogger(NocodeInitializer.class.getName());

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    if (isDeclarativeConfig(autoConfiguredOpenTelemetrySdk)) {
      processEmbeddedRules(getConfigProvider(autoConfiguredOpenTelemetrySdk));
    }
    processRulesFromExternalFile(getConfig(autoConfiguredOpenTelemetrySdk));
  }

  private static void processEmbeddedRules(ConfigProvider configProvider) {
    Objects.requireNonNull(configProvider);
    DeclarativeConfigProperties config = configProvider.getInstrumentationConfig("splunk");

    if (config != null) {
      List<DeclarativeConfigProperties> nocodeRules = config.getStructuredList("nocode");
      if (nocodeRules != null) {
        NocodeRules.setGlobalRules(YamlParser.parseFromDeclarativeConfig(nocodeRules));
      }
    }
  }

  private static void processRulesFromExternalFile(ConfigProperties config) {
    String yamlFileName = config.getString(NOCODE_YMLFILE);
    if (yamlFileName == null || yamlFileName.trim().isEmpty()) {
      return;
    }
    List<NocodeRules.Rule> instrumentationRules = Collections.emptyList();
    try {
      instrumentationRules = YamlParser.parseFromFile(yamlFileName);
      // can throw IllegalArgument and various other RuntimeExceptions too, not just IOException
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Can't load configured nocode yaml.", e);
    }
    NocodeRules.setGlobalRules(instrumentationRules);
  }
}
