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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.splunk.opentelemetry.jmx.JmxMetricInsightSupervisor;
import com.splunk.opentelemetry.opamp.effectiveconfig.EffectiveConfigReporter;
import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilingSupervisor;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingSupervisor;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.opamp.client.OpampClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.RemoteConfigStatuses;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoteConfigProcessorImplTest {
  @Mock ProfilingSupervisor profilingSupervisor;
  @Mock SnapshotProfilingSupervisor snapshotProfilingSupervisor;
  @Mock JmxMetricInsightSupervisor jmxMetricInsightSupervisor;
  @Mock EffectiveConfigReporter effectiveConfigReporter;
  @Mock OpampClient opampClient;
  private RemoteConfigProcessorImpl handler;

  @BeforeEach
  void setUp() {
    ProfilerConfiguration.SUPPLIER.configure(ProfilerConfiguration.defaultConfiguration());
    SnapshotProfilingConfiguration.SUPPLIER.configure(
        SnapshotProfilingConfiguration.defaultConfiguration());
    handler =
        new RemoteConfigProcessorImpl(
            profilingSupervisor,
            snapshotProfilingSupervisor,
            jmxMetricInsightSupervisor,
            effectiveConfigReporter);
  }

  @AfterEach
  void tearDown() {
    ProfilerConfiguration.SUPPLIER.reset();
    SnapshotProfilingConfiguration.SUPPLIER.reset();
  }

  @Test
  void shouldMarkRemoteConfigAsApplyingWhenProcessingStarts() {
    // given
    ByteString configHash = ByteString.encodeUtf8("test-config-hash");
    AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, "test-config:");

    // when
    handler.applyConfig(remoteConfig, opampClient);

    // then
    RemoteConfigStatus status = getInitialReportedRemoteConfigStatus();
    assertThat(status.last_remote_config_hash).isEqualTo(configHash);
    assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING);
    assertThat(status.error_message).isEmpty();
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
    RemoteConfigStatus status = getFinalReportedRemoteConfigStatus();
    assertThat(status.last_remote_config_hash).isEqualTo(configHash);
    assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
    assertThat(status.error_message).isEmpty();
    assertThat(ProfilerConfiguration.SUPPLIER.get().isEnabled()).isFalse();
    verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    verifyNoInteractions(
        profilingSupervisor, snapshotProfilingSupervisor, jmxMetricInsightSupervisor);
  }

  @Test
  void shouldReportErrorWhenRemoteConfigProcessingFailed() {
    // given
    String remoteConfigYaml =
        """
            distribution:
              splunk:
                profiling:
            INVALID YAML HERE
        """;
    ByteString configHash = ByteString.encodeUtf8("test-config-hash");
    AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, remoteConfigYaml);

    // when
    handler.applyConfig(remoteConfig, opampClient);

    // then
    RemoteConfigStatus status = getFinalReportedRemoteConfigStatus();
    assertThat(status.last_remote_config_hash).isEqualTo(configHash);
    assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
    assertThat(status.error_message).startsWith("Exception occurred:");
    assertThat(ProfilerConfiguration.SUPPLIER.get().isEnabled()).isFalse();
    assertThat(ProfilerConfiguration.SUPPLIER.get().getCallStackInterval().toMillis())
        .isEqualTo(10000);
    verifyNoInteractions(
        profilingSupervisor, snapshotProfilingSupervisor, jmxMetricInsightSupervisor);
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
    assertThat(ProfilerConfiguration.SUPPLIER.get().isEnabled()).isFalse();
    verifyNoInteractions(
        opampClient, profilingSupervisor, snapshotProfilingSupervisor, jmxMetricInsightSupervisor);
    verify(effectiveConfigReporter, never()).reportEffectiveConfigIfChanged();
  }

  @Nested
  class JmxConfigurationTest {
    @Test
    void forwardsInstrumentationJmxSettingsToSupervisor() throws Exception {
      AgentRemoteConfig remoteConfig =
          createRemoteConfig(
              ByteString.encodeUtf8("jmx-config-hash"),
              """
              instrumentation/development:
                java:
                  jmx:
                    discovery:
                      delay: 2000
                    rules:
                      - bean: test:type=Monitor
              """);

      handler.applyConfig(remoteConfig, opampClient);

      ArgumentCaptor<DeclarativeConfigProperties> configuration =
          ArgumentCaptor.forClass(DeclarativeConfigProperties.class);
      verify(jmxMetricInsightSupervisor).applyConfiguration(configuration.capture());
      assertThat(DeclarativeConfigProperties.toMap(configuration.getValue()))
          .isEqualTo(
              Map.of(
                  "discovery", Map.of("delay", 2000L),
                  "rules", List.of(Map.of("bean", "test:type=Monitor"))));
      assertThat(getFinalReportedRemoteConfigStatus().status)
          .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
      verifyNoInteractions(profilingSupervisor, snapshotProfilingSupervisor);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"jmx:", "jmx: {}"})
    void forwardsAnEmptyInstrumentationJmxNode(String jmxNode) throws Exception {
      AgentRemoteConfig remoteConfig =
          createRemoteConfig(
              ByteString.encodeUtf8("jmx-config-hash"),
              "instrumentation/development:\n  java:\n    " + jmxNode + "\n");

      handler.applyConfig(remoteConfig, opampClient);

      ArgumentCaptor<DeclarativeConfigProperties> configuration =
          ArgumentCaptor.forClass(DeclarativeConfigProperties.class);
      verify(jmxMetricInsightSupervisor).applyConfiguration(configuration.capture());
      assertThat(configuration.getValue().getPropertyKeys()).isEmpty();
      assertThat(getFinalReportedRemoteConfigStatus().status)
          .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
    }

    @Test
    void reportsFailureWhenSupervisorCannotApplyTheUpdate() throws Exception {
      doThrow(new IllegalArgumentException("Invalid JMX rules"))
          .when(jmxMetricInsightSupervisor)
          .applyConfiguration(any());
      AgentRemoteConfig remoteConfig =
          createRemoteConfig(
              ByteString.encodeUtf8("jmx-config-hash"),
              "instrumentation/development:\n  java:\n    jmx: {}\n");

      handler.applyConfig(remoteConfig, opampClient);

      RemoteConfigStatus status = getFinalReportedRemoteConfigStatus();
      assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
      assertThat(status.error_message).contains("Invalid JMX rules");
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "{}",
          "instrumentation/development: {}",
          "instrumentation/development:\n  java:\n    jdbc: {}\n"
        })
    void ignoresConfigurationWithoutInstrumentationJmxNode(String yaml) {
      handler.applyConfig(
          createRemoteConfig(ByteString.encodeUtf8("jmx-config-hash"), yaml), opampClient);

      verifyNoInteractions(
          jmxMetricInsightSupervisor, profilingSupervisor, snapshotProfilingSupervisor);
      assertThat(getFinalReportedRemoteConfigStatus().status)
          .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }
  }

  @Nested
  class AlwaysOnProfilerTest {
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
      RemoteConfigStatus status = getFinalReportedRemoteConfigStatus();
      assertThat(status.last_remote_config_hash).isEqualTo(configHash);
      assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
      assertThat(status.error_message).isEmpty();
      assertThat(ProfilerConfiguration.SUPPLIER.get().isEnabled()).isTrue();
      verify(profilingSupervisor).requestReinitializeProfiling();
      verifyNoMoreInteractions(profilingSupervisor);
      verifyNoInteractions(snapshotProfilingSupervisor);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }

    @Test
    void shouldUpdateProfilerConfigWhileRunning() {
      // given
      ProfilerConfiguration.SUPPLIER.configure(
          ProfilerConfiguration.builder().setEnabled(true).build());

      String remoteConfigYaml =
          """
              distribution:
                splunk:
                  profiling:
                    always_on:
                      cpu_profiler:
                        sampling_interval: 123
                        report_locks: true
                      memory_profiler:
          """;
      ByteString configHash = ByteString.encodeUtf8("test-config-hash");
      AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, remoteConfigYaml);

      // when
      handler.applyConfig(remoteConfig, opampClient);

      // then
      ProfilerConfiguration configuration = ProfilerConfiguration.SUPPLIER.get();
      assertThat(configuration.isEnabled()).isTrue();
      assertThat(configuration.getCallStackInterval().toMillis()).isEqualTo(123);
      assertThat(configuration.getLocksEnabled()).isTrue();
      assertThat(configuration.getMemoryEnabled()).isTrue();
      verify(profilingSupervisor).requestReinitializeProfiling();
      verifyNoMoreInteractions(profilingSupervisor);
      verifyNoInteractions(snapshotProfilingSupervisor);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }

    @Test
    void shouldStopProfilingWhenRemoteConfigDisablesProfiler() {
      // given
      ProfilerConfiguration.SUPPLIER.configure(
          ProfilerConfiguration.builder().setEnabled(true).build());
      String remoteConfigYaml =
          """
              distribution:
                splunk:
                  profiling:
                    always_on:
          """;
      ByteString configHash = ByteString.encodeUtf8("test-config-hash");
      AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, remoteConfigYaml);

      // when
      handler.applyConfig(remoteConfig, opampClient);

      // then
      RemoteConfigStatus status = getFinalReportedRemoteConfigStatus();
      assertThat(status.last_remote_config_hash).isEqualTo(configHash);
      assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
      assertThat(status.error_message).isEmpty();
      assertThat(ProfilerConfiguration.SUPPLIER.get().isEnabled()).isFalse();
      verify(profilingSupervisor).requestReinitializeProfiling();
      verify(profilingSupervisor, never()).requestStartProfiling();
      verifyNoInteractions(snapshotProfilingSupervisor);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }

    @Test
    void shouldNotReconfigureProfilingWhenThereIsNoAlwaysOnProfilerConfig() {
      // given
      ProfilerConfiguration initialConfiguration =
          ProfilerConfiguration.builder().setEnabled(true).build();
      ProfilerConfiguration.SUPPLIER.configure(initialConfiguration);
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
      assertThat(ProfilerConfiguration.SUPPLIER.get()).isSameAs(initialConfiguration);
      verifyNoInteractions(profilingSupervisor);
    }
  }

  @Nested
  class SnapshotProfilingSupervisorTest {
    @Test
    void shouldStartProfilingWhenRemoteConfigEnablesProfiler() {
      // given
      String remoteConfigYaml =
          """
              distribution:
                splunk:
                  profiling:
                    callgraphs:
          """;
      ByteString configHash = ByteString.encodeUtf8("test-config-hash");
      AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, remoteConfigYaml);

      // when
      handler.applyConfig(remoteConfig, opampClient);

      // then
      RemoteConfigStatus status = getFinalReportedRemoteConfigStatus();
      assertThat(status.last_remote_config_hash).isEqualTo(configHash);
      assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
      assertThat(status.error_message).isEmpty();
      assertThat(SnapshotProfilingConfiguration.SUPPLIER.get().isEnabled()).isTrue();
      verify(snapshotProfilingSupervisor).requestReinitializeProfiling();
      verifyNoMoreInteractions(snapshotProfilingSupervisor);
      verifyNoInteractions(profilingSupervisor);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }

    @Test
    void shouldNotReinitializeProfilingWhenConfigurationDoesNotChange() {
      // given
      SnapshotProfilingConfiguration.SUPPLIER.configure(
          SnapshotProfilingConfiguration.builder().setEnabled(true).build());
      String remoteConfigYaml =
          """
              distribution:
                splunk:
                  profiling:
                    callgraphs:
          """;
      ByteString configHash = ByteString.encodeUtf8("test-config-hash");
      AgentRemoteConfig remoteConfig = createRemoteConfig(configHash, remoteConfigYaml);

      // when
      handler.applyConfig(remoteConfig, opampClient);

      // then
      assertThat(SnapshotProfilingConfiguration.SUPPLIER.get().isEnabled()).isTrue();
      verifyNoInteractions(profilingSupervisor, snapshotProfilingSupervisor);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }

    @Test
    void shouldUpdateCallgraphsSettingsFromRemoteConfig() {
      SnapshotProfilingConfiguration.SUPPLIER.configure(
          SnapshotProfilingConfiguration.builder().setEnabled(true).build());
      String remoteConfigYaml =
          """
          distribution:
            splunk:
              profiling:
                callgraphs:
                  selection_probability: 0.5
                  sampling_interval: 123
                  report_locks: true
          """;
      AgentRemoteConfig remoteConfig =
          createRemoteConfig(ByteString.encodeUtf8("test-config-hash"), remoteConfigYaml);

      handler.applyConfig(remoteConfig, opampClient);

      SnapshotProfilingConfiguration configuration = SnapshotProfilingConfiguration.SUPPLIER.get();
      assertThat(configuration.getSnapshotSelectionProbability()).isEqualTo(0.5);
      assertThat(configuration.getSamplingInterval()).isEqualTo(Duration.ofMillis(123));
      assertThat(configuration.getLocksEnabled()).isTrue();
      verify(snapshotProfilingSupervisor).requestReinitializeProfiling();
      verifyNoMoreInteractions(snapshotProfilingSupervisor);
      verifyNoInteractions(profilingSupervisor);
    }

    @Test
    void shouldStopProfilingWhenRemoteConfigDisablesProfiler() {
      // given
      SnapshotProfilingConfiguration.SUPPLIER.configure(
          SnapshotProfilingConfiguration.builder().setEnabled(true).build());
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
      RemoteConfigStatus status = getFinalReportedRemoteConfigStatus();
      assertThat(status.last_remote_config_hash).isEqualTo(configHash);
      assertThat(status.status).isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
      assertThat(status.error_message).isEmpty();
      assertThat(SnapshotProfilingConfiguration.SUPPLIER.get().isEnabled()).isFalse();
      verify(snapshotProfilingSupervisor).requestReinitializeProfiling();
      verifyNoMoreInteractions(snapshotProfilingSupervisor);
      verifyNoInteractions(profilingSupervisor);
      verify(effectiveConfigReporter).reportEffectiveConfigIfChanged();
    }
  }

  private RemoteConfigStatus getInitialReportedRemoteConfigStatus() {
    return getReportedRemoteConfigStatuses().get(0);
  }

  private RemoteConfigStatus getFinalReportedRemoteConfigStatus() {
    return getReportedRemoteConfigStatuses().get(1);
  }

  private List<RemoteConfigStatus> getReportedRemoteConfigStatuses() {
    ArgumentCaptor<RemoteConfigStatus> statusCaptor =
        ArgumentCaptor.forClass(RemoteConfigStatus.class);
    verify(opampClient, times(2)).setRemoteConfigStatus(statusCaptor.capture());
    return statusCaptor.getAllValues();
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
