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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.AlwaysOnProfilerStateListener;
import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilerStateListener;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.HashMap;
import java.util.Map;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;

public class EffectiveConfigReporter
    implements AlwaysOnProfilerStateListener, SnapshotProfilerStateListener {
  private final UpdatableEffectiveConfigState effectiveConfigState;
  private final EffectiveConfigFileFactory effectiveConfigFactory;
  private ProfilerConfiguration profilerConfiguration =
      ProfilerConfiguration.defaultConfiguration();
  private SnapshotProfilingConfiguration snapshotConfiguration =
      SnapshotProfilingConfiguration.defaultConfiguration();
  private String lastReportedConfigContent;

  @VisibleForTesting
  EffectiveConfigReporter(
      EffectiveConfigFileFactory effectiveConfigFactory,
      UpdatableEffectiveConfigState effectiveConfigState) {
    this.effectiveConfigFactory = effectiveConfigFactory;
    this.effectiveConfigState = effectiveConfigState;
  }

  public static EffectiveConfigReporter create(
      AutoConfiguredOpenTelemetrySdk sdk, UpdatableEffectiveConfigState effectiveConfigState) {
    return new EffectiveConfigReporter(createEffectiveConfigFactory(sdk), effectiveConfigState);
  }

  @Override
  public synchronized void onAlwaysOnProfilerStateChanged(
      ProfilerConfiguration effectiveConfiguration) {
    profilerConfiguration = effectiveConfiguration;
    reportEffectiveConfigIfChanged();
  }

  @Override
  public synchronized void onSnapshotProfilerStateChanged(
      SnapshotProfilingConfiguration effectiveConfiguration) {
    snapshotConfiguration = effectiveConfiguration;
    reportEffectiveConfigIfChanged();
  }

  public synchronized boolean reportEffectiveConfigIfChanged() {
    // Detect if effectiveConfig changed and needs to be reported
    String configContent =
        effectiveConfigFactory.createEffectiveConfigContent(
            profilerConfiguration, snapshotConfiguration);
    if (configContent.equals(lastReportedConfigContent)) {
      return false;
    }

    Map<String, AgentConfigFile> configMap = new HashMap<>();
    AgentConfigFile configFile =
        new AgentConfigFile(
            new ByteString(configContent.getBytes(UTF_8)), effectiveConfigFactory.getContentType());
    configMap.put(effectiveConfigFactory.getFileName(), configFile);

    effectiveConfigState.set(new AgentConfigMap(configMap));
    lastReportedConfigContent = configContent;

    return true;
  }

  private static EffectiveConfigFileFactory createEffectiveConfigFactory(
      AutoConfiguredOpenTelemetrySdk sdk) {
    if (AutoConfigureUtil.isDeclarativeConfig(sdk)) {
      return new DeclarativeEffectiveConfigFileFactory();
    }
    return new EnvVarsEffectiveConfigFileFactory(getConfig(sdk));
  }
}
