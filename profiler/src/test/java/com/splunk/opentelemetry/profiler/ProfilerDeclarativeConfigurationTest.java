package com.splunk.opentelemetry.profiler;

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.extension.incubator.fileconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfilerDeclarativeConfigurationTest {

  @Test
  void shouldMapYamlToConfiguration() {
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      always_on:
                        include_agent_internals: true
                        include_jvm_internals: true
                        tracing_stacks_only: true
                        stack_depth_limit: 73
                        keep_recording_files: true
                        recording_directory: "/tmp/prof"
                        recording_duration: 12345

                        cpu_profiler:
                          sampling_interval: 1410
                        memory_profiler:
                          event_rate: "250/s"
                          native_sampling: true
            """);

    DeclarativeConfigProperties profilingConfig = getProfilingConfig(model);

    ProfilerDeclarativeConfiguration config = new ProfilerDeclarativeConfiguration(profilingConfig);

    assertThat(config.isEnabled()).isTrue();
    assertThat(config.getIncludeAgentInternalStacks()).isTrue();
    assertThat(config.getIncludeJvmInternalStacks()).isTrue();
    assertThat(config.getTracingStacksOnly()).isTrue();
    assertThat(config.getStackDepth()).isEqualTo(73);
    assertThat(config.getKeepFiles()).isTrue();
    assertThat(config.getProfilerDirectory()).isEqualTo("/tmp/prof");
    assertThat(config.getRecordingDuration()).isEqualTo(Duration.ofMillis(12345));
    assertThat(config.getCallStackInterval()).isEqualTo(Duration.ofMillis(1410));
    assertThat(config.getMemoryEnabled()).isTrue();
    assertThat(config.getMemoryEventRateLimitEnabled()).isTrue();
    assertThat(config.getMemoryEventRate()).isEqualTo("250/s");
    assertThat(config.getUseAllocationSampleEvent()).isTrue();

    assertThrows(UnsupportedOperationException.class, config::getIngestUrl);
    assertThrows(UnsupportedOperationException.class, config::getOtlpProtocol);
  }

  private static DeclarativeConfigProperties getProfilingConfig(OpenTelemetryConfigurationModel model) {
    Map<String, Object> properties =
        model.getInstrumentationDevelopment().getJava().getAdditionalProperties();
    ComponentLoader componentLoader =
        ComponentLoader.forClassLoader(DeclarativeConfigProperties.class.getClassLoader());
    DeclarativeConfigProperties declarativeConfigProperties =
        YamlDeclarativeConfigProperties.create(properties, componentLoader);
    return declarativeConfigProperties
        .getStructured("distribution", empty())
        .getStructured("splunk", empty())
        .getStructured("profiling", empty());
  }
}
