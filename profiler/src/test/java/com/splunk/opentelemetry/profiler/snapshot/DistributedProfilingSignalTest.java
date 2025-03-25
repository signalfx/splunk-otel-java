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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.profiler.snapshot.simulation.ExitCall;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Message;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Server;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import java.time.Duration;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DistributedProfilingSignalTest {
  private final RecordingTraceRegistry downstreamRegistry = new RecordingTraceRegistry();
  private final SnapshotProfilingSdkCustomizer downstreamCustomizer =
      Snapshotting.customizer().with(downstreamRegistry).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension downstreamSdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(downstreamCustomizer)
          .with(new SnapshotVolumePropagator(() -> true))
          .build();

  @RegisterExtension
  public final Server downstream =
      Server.builder(downstreamSdk)
          .named("downstream")
          .performing(delayOf(Duration.ofMillis(250)))
          .build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension middleSdk = OpenTelemetrySdkExtension.configure().build();

  @RegisterExtension
  public final Server middle =
      Server.builder(middleSdk).named("middle").performing(ExitCall.to(downstream)).build();

  private final RecordingTraceRegistry upstreamRegistry = new RecordingTraceRegistry();
  private final SnapshotProfilingSdkCustomizer upstreamCustomizer =
      Snapshotting.customizer().with(upstreamRegistry).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension upstreamSdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(upstreamCustomizer)
          .with(new SnapshotVolumePropagator(() -> true))
          .build();

  @RegisterExtension
  public final Server upstream =
      Server.builder(upstreamSdk).named("upstream").performing(ExitCall.to(middle)).build();

  /**
   * The test below is asserting a few things are happening. First, consider the following
   * distributed system.
   *
   * <pre>{@code
   * +----------+    +--------+    +------------+
   * | upstream | -> | middle | -> | downstream |
   * +----------+    +--------+    +------------+
   * }</pre>
   *
   * <p>1. Upstream is instrumented with the snapshot profiling agent extension <br>
   * 2. Middle is instrumented with a vanilla OpenTelemetry agent <br>
   * 3. Downstream is instrumented with the snapshot profiling agent extension <br>
   * We want for the upstream service instrumentation to initially register the trace for profiling
   * and for that signal to propagate through the middle service to the downstream service.
   */
  @Test
  void traceSnapshotVolumePropagatesAcrossProcessBoundaries() {
    var message = new Message();

    upstream.send(message);

    await().atMost(Duration.ofSeconds(2)).until(() -> upstream.waitForResponse() != null);
    assertThat(upstreamRegistry.registeredTraceIds()).isNotEmpty();
    assertThat(downstreamRegistry.registeredTraceIds()).isNotEmpty();
    assertEquals(upstreamRegistry.registeredTraceIds(), downstreamRegistry.registeredTraceIds());
  }

  private UnaryOperator<Message> delayOf(Duration duration) {
    return message -> {
      try {
        Thread.sleep(duration.toMillis());
        return message;
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
