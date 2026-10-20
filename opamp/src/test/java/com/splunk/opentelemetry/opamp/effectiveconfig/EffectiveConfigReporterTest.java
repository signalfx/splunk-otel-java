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

import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
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

  @Mock private EffectiveConfigFileFactory effectiveConfigFactory;
  @Mock private UpdatableEffectiveConfigState effectiveConfigState;

  private final ProfilerConfiguration profilerConfiguration =
      ProfilerConfiguration.builder().setEnabled(false).build();
  private final SnapshotProfilingConfiguration snapshotConfiguration =
      SnapshotProfilingConfiguration.builder().setEnabled(false).build();

  private EffectiveConfigReporter reporter;

  @BeforeEach
  void setUp() {
    reporter = new EffectiveConfigReporter(effectiveConfigFactory, effectiveConfigState);
  }

  @Test
  void reportEffectiveConfigIfChanged_reportsGeneratedConfig() {
    stubConfig("first-config");

    notifyInitialConfigurations();

    AgentConfigFile configFile = captureReportedConfigFile();
    assertThat(configFile.body.utf8()).isEqualTo("first-config");
    assertThat(configFile.content_type).isEqualTo(CONTENT_TYPE);
    verify(effectiveConfigFactory, times(2))
        .createEffectiveConfigContent(profilerConfiguration, snapshotConfiguration);
  }

  @Test
  void reportEffectiveConfigIfChanged_skipsUnchangedConfig() {
    stubConfig("same-config");

    notifyInitialConfigurations();
    boolean reported = reporter.reportEffectiveConfigIfChanged();
    reporter.onAlwaysOnProfilerStateChanged(profilerConfiguration);
    reporter.onSnapshotProfilerStateChanged(snapshotConfiguration);

    assertThat(reported).isFalse();
    verify(effectiveConfigState, times(1)).set(any());
  }

  @Test
  void reportEffectiveConfigIfChanged_reportsUpdatedConfig() {
    stubConfig("first-config", "second-config");

    reporter.onAlwaysOnProfilerStateChanged(profilerConfiguration);
    boolean secondReport = reporter.reportEffectiveConfigIfChanged();

    assertThat(secondReport).isTrue();

    ArgumentCaptor<AgentConfigMap> configMapCaptor = ArgumentCaptor.forClass(AgentConfigMap.class);
    verify(effectiveConfigState, times(2)).set(configMapCaptor.capture());
    assertThat(configMapCaptor.getAllValues())
        .extracting(configMap -> configMap.config_map.get(CONFIG_FILE_NAME).body.utf8())
        .containsExactly("first-config", "second-config");
  }

  @Test
  void reportsChangesFromBothProfilersAndSkipsUnchangedNotifications() {
    stubConfig("initial", "initial", "always-on-started", "both-started", "both-started");
    notifyInitialConfigurations();

    ProfilerConfiguration activeProfilerConfiguration =
        profilerConfiguration.toBuilder().setEnabled(true).build();
    reporter.onAlwaysOnProfilerStateChanged(activeProfilerConfiguration);
    SnapshotProfilingConfiguration activeSnapshotConfiguration =
        snapshotConfiguration.toBuilder().setEnabled(true).build();
    reporter.onSnapshotProfilerStateChanged(activeSnapshotConfiguration);
    reporter.onSnapshotProfilerStateChanged(activeSnapshotConfiguration);

    ArgumentCaptor<AgentConfigMap> configMaps = ArgumentCaptor.forClass(AgentConfigMap.class);
    verify(effectiveConfigState, times(3)).set(configMaps.capture());
    assertThat(configMaps.getAllValues())
        .extracting(configMap -> configMap.config_map.get(CONFIG_FILE_NAME).body.utf8())
        .containsExactly("initial", "always-on-started", "both-started");
    verify(effectiveConfigFactory)
        .createEffectiveConfigContent(activeProfilerConfiguration, snapshotConfiguration);
    verify(effectiveConfigFactory, times(2))
        .createEffectiveConfigContent(activeProfilerConfiguration, activeSnapshotConfiguration);
  }

  @Test
  void reportsFirstListenerConfigurationWithOtherProfilerDisabled() {
    stubConfig("initial");
    SnapshotProfilingConfiguration activeSnapshotConfiguration =
        snapshotConfiguration.toBuilder().setEnabled(true).build();
    reporter.onSnapshotProfilerStateChanged(activeSnapshotConfiguration);

    assertThat(captureReportedConfigFile().body.utf8()).isEqualTo("initial");
    verify(effectiveConfigFactory)
        .createEffectiveConfigContent(profilerConfiguration, activeSnapshotConfiguration);
  }

  private void stubConfig(String firstConfig, String... subsequentConfigs) {
    when(effectiveConfigFactory.getFileName()).thenReturn(CONFIG_FILE_NAME);
    when(effectiveConfigFactory.getContentType()).thenReturn(CONTENT_TYPE);
    when(effectiveConfigFactory.createEffectiveConfigContent(any(), any()))
        .thenReturn(firstConfig, subsequentConfigs);
  }

  private void notifyInitialConfigurations() {
    reporter.onAlwaysOnProfilerStateChanged(profilerConfiguration);
    reporter.onSnapshotProfilerStateChanged(snapshotConfiguration);
  }

  private AgentConfigFile captureReportedConfigFile() {
    ArgumentCaptor<AgentConfigMap> configMapCaptor = ArgumentCaptor.forClass(AgentConfigMap.class);
    verify(effectiveConfigState).set(configMapCaptor.capture());
    AgentConfigMap configMap = configMapCaptor.getValue();
    assertThat(configMap.config_map).containsOnlyKeys(CONFIG_FILE_NAME);
    return configMap.config_map.get(CONFIG_FILE_NAME);
  }
}
