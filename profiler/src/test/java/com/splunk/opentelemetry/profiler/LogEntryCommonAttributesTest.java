package com.splunk.opentelemetry.profiler;

import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.common.Attributes;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogEntryCommonAttributesTest {

    @Test
    void testBuild() {
        String sourceType = "otel.profiling";
        String eventName = "core.core.unicorn";
        Attributes expected = Attributes.builder()
                .put(SOURCE_TYPE, sourceType)
                .put(SOURCE_EVENT_NAME, eventName)
                .put(SOURCE_EVENT_PERIOD, 999L)
                .build();

        EventPeriods periods = mock(EventPeriods.class);
        RecordedEvent event = mock(RecordedEvent.class);
        EventType eventType = mock(EventType.class);

        when(event.getEventType()).thenReturn(eventType);
        when(eventType.getName()).thenReturn(eventName);
        when(periods.getDuration(eventName)).thenReturn(Duration.ofMillis(999));

        LogEntryCommonAttributes logEntryAttributes = new LogEntryCommonAttributes(periods);

        Attributes result = logEntryAttributes.build(event);
        assertEquals(expected, result);
    }

}