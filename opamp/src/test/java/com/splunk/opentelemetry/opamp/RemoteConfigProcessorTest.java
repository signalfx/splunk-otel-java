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

package com.splunk.opentelemetry.opamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.splunk.opentelemetry.opamp.effectiveconfig.EffectiveConfigReporter;
import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilingSupervisor;
import io.opentelemetry.opamp.client.OpampClient;
import java.util.Map;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.RemoteConfigStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoteConfigProcessorTest {
  @Mock ProfilerConfiguration profilerConfiguration;
  @Mock ProfilingSupervisor profilingSupervisor;
  @Mock EffectiveConfigReporter effectiveConfigReporter;
  @Mock OpampClient opampClient;
  private ProfilerRemoteConfiguration profilerRemoteConfiguration;
  private RemoteConfigProcessor handler;

  @BeforeEach
  void setUp() {
    profilerRemoteConfiguration = new ProfilerRemoteConfiguration(profilerConfiguration);
    handler =
        new RemoteConfigProcessor(
            profilerRemoteConfiguration, profilingSupervisor, effectiveConfigReporter);
  }

  @Test
  void shouldMarkRemoteConfigAsAppliedWhenProfilingConfigIsNotProvided() {
    // given
    String remoteConfigYaml = "test-config:";
    ByteString configHash = ByteString.encodeUtf8("test-config-hash");
    AgentRemoteConfig remoteConfig =
        createRemoteConfig(
            configHash,
            Map.of(
                "splunk.remote.config",
                new AgentConfigFile.Builder()
                    .body(ByteString.encodeUtf8(remoteConfigYaml))
                    .build()));

    // when
    handler.applyConfig(remoteConfig, opampClient);

    // then
    RemoteConfigStatus status = getReportedRemoteConfigStatus();
    assertThat(status.last_remote_config_hash).isEqualTo(configHash);
    assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
    assertThat(status.error_message).isEmpty();
    assertThat(profilerRemoteConfiguration.isEnabled()).isFalse();
    verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    verifyNoInteractions(profilingSupervisor);
  }

  @Test
  void shouldStartProfilingWhenRemoteConfigEnablesProfiler() {
    // given
    String remoteConfigYaml =
        """
        distribution:
          splunk:
            profiling:
              always_on:
                cpu_profiler:
    """;
    ByteString configHash = ByteString.encodeUtf8("test-config-hash");
    AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, remoteConfigYaml);

    // when
    handler.applyConfig(remoteConfig, opampClient);

    // then
    RemoteConfigStatus status = getReportedRemoteConfigStatus();
    assertThat(status.last_remote_config_hash).isEqualTo(configHash);
    assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
    assertThat(status.error_message).isEmpty();
    assertThat(profilerRemoteConfiguration.isEnabled()).isTrue();
    verify(profilingSupervisor).requestStartProfiling();
    verify(profilingSupervisor, never()).requestStopProfiling();
    verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
  }

  @Test
  void shouldStopProfilingWhenRemoteConfigDisablesProfiler() {
    // given
    String remoteConfigYaml =
        """
        distribution:
          splunk:
            profiling:
    """;
    ByteString configHash = ByteString.encodeUtf8("test-config-hash");
    AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, remoteConfigYaml);

    // when
    handler.applyConfig(remoteConfig, opampClient);

    // then
    RemoteConfigStatus status = getReportedRemoteConfigStatus();
    assertThat(status.last_remote_config_hash).isEqualTo(configHash);
    assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
    assertThat(status.error_message).isEmpty();
    assertThat(profilerRemoteConfiguration.isEnabled()).isFalse();
    verify(profilingSupervisor).requestStopProfiling();
    verify(profilingSupervisor, never()).requestStartProfiling();
    verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
  }

  @Test
  void shouldIgnoreRemoteConfigWithoutExpectedConfigFile() {
    // given
    AgentRemoteConfig remoteConfig =
        createRemoteConfig(
            ByteString.encodeUtf8("test-config-hash"),
            Map.of(
                "other.config",
                new AgentConfigFile.Builder().body(ByteString.encodeUtf8("test-config")).build()));

    // when
    handler.applyConfig(remoteConfig, opampClient);

    // then
    assertThat(profilerRemoteConfiguration.isEnabled()).isFalse();
    verifyNoInteractions(opampClient, profilingSupervisor);
    verify(effectiveConfigReporter, never()).reportEffectiveConfigIfChanged();
  }

  private RemoteConfigStatus getReportedRemoteConfigStatus() {
    ArgumentCaptor<RemoteConfigStatus> statusCaptor =
        ArgumentCaptor.forClass(RemoteConfigStatus.class);
    verify(opampClient).setRemoteConfigStatus(statusCaptor.capture());
    return statusCaptor.getValue();
  }

  private static AgentRemoteConfig createRemoteConfig(ByteString configHash, String config) {
    return createRemoteConfig(
        configHash,
        Map.of(
            "splunk.remote.config",
            new AgentConfigFile.Builder().body(ByteString.encodeUtf8(config)).build()));
  }

  private static AgentRemoteConfig createRemoteConfig(
      ByteString configHash, Map<String, AgentConfigFile> configMap) {
    return new AgentRemoteConfig.Builder()
        .config_hash(configHash)
        .config(new AgentConfigMap.Builder().config_map(configMap).build())
        .build();
  }
}
