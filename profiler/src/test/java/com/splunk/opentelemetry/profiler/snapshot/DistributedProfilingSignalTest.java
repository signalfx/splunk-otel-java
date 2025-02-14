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
import io.opentelemetry.api.trace.SpanContext;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DistributedProfilingSignalTest {
  private final RecordingTraceRegistry downstreamRegistry = new RecordingTraceRegistry();
  private final SnapshotProfilingSdkCustomizer downstreamCustomizer =
      new SnapshotProfilingSdkCustomizer(downstreamRegistry);

  @RegisterExtension
  public final OpenTelemetrySdkExtension downstreamSdk =
      OpenTelemetrySdkExtension.builder()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(downstreamCustomizer)
          .build();

  @RegisterExtension
  public final Server downstream =
      Server.builder(downstreamSdk)
          .named("downstream")
          .performing(delayOf(Duration.ofMillis(250)))
          .build();

  private final RecordingTraceRegistry upstreamRegistry = new RecordingTraceRegistry();
  private final SnapshotProfilingSdkCustomizer upstreamCustomizer =
      new SnapshotProfilingSdkCustomizer(upstreamRegistry);

  @RegisterExtension
  public final OpenTelemetrySdkExtension upstreamSdk =
      OpenTelemetrySdkExtension.builder()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(upstreamCustomizer)
          .build();

  @RegisterExtension
  public final Server upstream =
      Server.builder(upstreamSdk).named("upstream").performing(ExitCall.to(downstream)).build();

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

  private static class RecordingTraceRegistry extends TraceRegistry {
    private final Set<String> registeredTraceIds = new HashSet<>();

    @Override
    public void register(SpanContext spanContext) {
      registeredTraceIds.add(spanContext.getTraceId());
      super.register(spanContext);
    }

    @Override
    public boolean isRegistered(SpanContext spanContext) {
      return super.isRegistered(spanContext);
    }

    @Override
    public void unregister(SpanContext spanContext) {
      super.unregister(spanContext);
    }

    Set<String> registeredTraceIds() {
      return Collections.unmodifiableSet(registeredTraceIds);
    }
  }
}
