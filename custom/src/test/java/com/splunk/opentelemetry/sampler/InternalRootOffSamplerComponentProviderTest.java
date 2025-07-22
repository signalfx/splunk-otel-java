package com.splunk.opentelemetry.sampler;

import static com.splunk.opentelemetry.DeclarativeConfigTestUtil.createAutoConfiguredSdk;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InternalRootOffSamplerComponentProviderTest
{
  @Test
  void shouldCreateSampler(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              sampler:
                internal_root_off:
            """;

    OpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir).getOpenTelemetrySdk();
    assertThat(sdk.getSdkTracerProvider().getSampler()).isInstanceOf(InternalRootOffSampler.class);
  }
}
