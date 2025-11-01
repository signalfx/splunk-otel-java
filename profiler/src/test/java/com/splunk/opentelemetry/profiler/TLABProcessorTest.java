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

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_MEMORY_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.allocation.sampler.RateLimitingAllocationEventSampler;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TLABProcessorTest {

  public static final long ONE_MB = 1024 * 1024L;
  public static final long THREAD_ID = 606L;

  @Mock EventReader eventReader;

  @Test
  void testNullStack() {
    IItem event = mock(IItem.class);
    when(eventReader.getStackTrace(event)).thenReturn(null); // just to be explicit

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false)).thenReturn(true);

    TLABProcessor processor = TLABProcessor.builder(config).eventReader(eventReader).build();
    processor.accept(event);
    // success, no NPEs
  }

  @Test
  void testProfilingDisabled() {
    IItem event =
        mock(
            IItem.class,
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new IllegalStateException(
                    "IItem methods should not be called when TLAB profiling is not enabled");
              }
            });

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false)).thenReturn(false);

    TLABProcessor processor = TLABProcessor.builder(config).build();
    processor.accept(event);
  }

  @Test
  void testProcess() {
    Instant now = Instant.now();
    StackSerializer serializer = mock(StackSerializer.class);

    IItem event = createMockEvent(serializer, now);

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false)).thenReturn(true);

    SpanContext spanContext =
        SpanContext.create(
            TraceId.fromLongs(123, 456),
            SpanId.fromLong(123),
            TraceFlags.getSampled(),
            TraceState.getDefault());
    SpanContextualizer spanContextualizer = mock(SpanContextualizer.class);
    when(spanContextualizer.link(THREAD_ID)).thenReturn(new SpanLinkage(spanContext, THREAD_ID));

    TestAllocationEventExporter allocationEventExporter = new TestAllocationEventExporter();

    TLABProcessor processor =
        TLABProcessor.builder(config)
            .eventReader(eventReader)
            .allocationEventExporter(allocationEventExporter)
            .spanContextualizer(spanContextualizer)
            .build();

    processor.accept(event);

    assertThat(allocationEventExporter.events).isNotEmpty();
  }

  private IItem createMockEvent(StackSerializer serializer, Instant now) {
    String stackAsString = "i am a serialized stack believe me";

    IItem event = mock(IItem.class);
    IMCStackTrace stack = mock(IMCStackTrace.class);
    IType eventType = mock(IType.class);
    IMCThread mockThread = mock(IMCThread.class);

    when(eventReader.getStartInstant(event)).thenReturn(now);
    when(eventReader.getStackTrace(event)).thenReturn(stack);
    when(event.getType()).thenReturn(eventType);
    when(eventReader.getAllocationSize(event)).thenReturn(ONE_MB);
    when(eventReader.getThread(event)).thenReturn(mockThread);
    when(mockThread.getThreadId()).thenReturn(THREAD_ID);
    when(mockThread.getThreadName()).thenReturn("mockingbird");
    when(stack.getTruncationState()).thenReturn(IMCStackTrace.TruncationState.NOT_TRUNCATED);
    when(eventType.getIdentifier()).thenReturn("tee-lab");
    when(serializer.serialize(stack)).thenReturn(stackAsString);

    return event;
  }

  @Test
  void testSampling() {
    StackSerializer serializer = mock(StackSerializer.class);
    SpanContextualizer spanContextualizer = mock(SpanContextualizer.class);
    when(spanContextualizer.link(anyLong())).thenReturn(SpanLinkage.NONE);

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false)).thenReturn(true);

    TestAllocationEventExporter allocationEventExporter = new TestAllocationEventExporter();

    RateLimitingAllocationEventSampler sampler = new RateLimitingAllocationEventSampler("100/s");
    TLABProcessor processor =
        new TLABProcessor.Builder(true)
            .eventReader(eventReader)
            .allocationEventExporter(allocationEventExporter)
            .spanContextualizer(spanContextualizer)
            .sampler(sampler)
            .build();

    IItem event = createMockEvent(serializer, Instant.now());

    for (int i = 0; i < 10; i++) {
      sampler.updateSampler(i % 2 == 0 ? 1.0 : 0.0);
      processor.accept(event);

      if (i % 2 == 0) {
        assertThat(allocationEventExporter.events).isNotEmpty();
      } else {
        assertThat(allocationEventExporter.events).isEmpty();
      }

      allocationEventExporter.reset();
    }
  }

  private static class TestAllocationEventExporter implements AllocationEventExporter {
    List<IItem> events = new ArrayList<>();

    @Override
    public void export(IItem event, AllocationEventSampler sampler, SpanContext spanContext) {
      events.add(event);
    }

    void reset() {
      events.clear();
    }
  }
}
