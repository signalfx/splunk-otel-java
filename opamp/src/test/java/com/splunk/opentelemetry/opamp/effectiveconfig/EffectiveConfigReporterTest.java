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

package com.splunk.opentelemetry.opamp.effectiveconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EffectiveConfigReporterTest {
  private static final String CONFIG_FILE_NAME = "splunk-effective-config.properties";
  private static final String CONTENT_TYPE = "text/plain; format=properties; vendor=splunk";

  @Mock private EffectiveConfigFactory effectiveConfigFactory;
  @Mock private UpdatableEffectiveConfigState effectiveConfigState;

  private EffectiveConfigReporter reporter;

  @BeforeEach
  void setUp() {
    when(effectiveConfigFactory.getFileName()).thenReturn(CONFIG_FILE_NAME);
    when(effectiveConfigFactory.getContentType()).thenReturn(CONTENT_TYPE);
    reporter = new EffectiveConfigReporter(effectiveConfigFactory, effectiveConfigState);
  }

  @Test
  void reportEffectiveConfigIfChanged_reportsGeneratedConfig() {
    when(effectiveConfigFactory.createEffectiveConfigContent()).thenReturn("first-config");

    boolean reported = reporter.reportEffectiveConfigIfChanged();

    assertThat(reported).isTrue();
    AgentConfigFile configFile = captureReportedConfigFile();
    assertThat(configFile.body.utf8()).isEqualTo("first-config");
    assertThat(configFile.content_type).isEqualTo(CONTENT_TYPE);
  }

  @Test
  void reportEffectiveConfigIfChanged_skipsUnchangedConfig() {
    when(effectiveConfigFactory.createEffectiveConfigContent()).thenReturn("same-config");

    boolean firstReport = reporter.reportEffectiveConfigIfChanged();
    boolean secondReport = reporter.reportEffectiveConfigIfChanged();

    assertThat(firstReport).isTrue();
    assertThat(secondReport).isFalse();
    verify(effectiveConfigState, times(1)).set(any());
  }

  @Test
  void reportEffectiveConfigIfChanged_reportsUpdatedConfig() {
    when(effectiveConfigFactory.createEffectiveConfigContent())
        .thenReturn("first-config", "second-config");

    boolean firstReport = reporter.reportEffectiveConfigIfChanged();
    boolean secondReport = reporter.reportEffectiveConfigIfChanged();

    assertThat(firstReport).isTrue();
    assertThat(secondReport).isTrue();

    ArgumentCaptor<AgentConfigMap> configMapCaptor = ArgumentCaptor.forClass(AgentConfigMap.class);
    verify(effectiveConfigState, times(2)).set(configMapCaptor.capture());
    assertThat(configMapCaptor.getAllValues())
        .extracting(configMap -> configMap.config_map.get(CONFIG_FILE_NAME).body.utf8())
        .containsExactly("first-config", "second-config");
  }

  private AgentConfigFile captureReportedConfigFile() {
    ArgumentCaptor<AgentConfigMap> configMapCaptor = ArgumentCaptor.forClass(AgentConfigMap.class);
    verify(effectiveConfigState).set(configMapCaptor.capture());
    AgentConfigMap configMap = configMapCaptor.getValue();
    assertThat(configMap.config_map).containsOnlyKeys(CONFIG_FILE_NAME);
    return configMap.config_map.get(CONFIG_FILE_NAME);
  }
}
