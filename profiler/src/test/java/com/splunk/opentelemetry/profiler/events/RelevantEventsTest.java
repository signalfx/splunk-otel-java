package com.splunk.opentelemetry.profiler.events;

import com.splunk.opentelemetry.profiler.Configuration;
import com.splunk.opentelemetry.profiler.TLABProcessor;
import com.splunk.opentelemetry.profiler.ThreadDumpProcessor;
import io.opentelemetry.instrumentation.api.config.Config;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelevantEventsTest {

    RecordedEvent threadDump;
    RecordedEvent tlab;

    @BeforeEach
    void setup() {
        threadDump = mock(RecordedEvent.class);
        tlab = mock(RecordedEvent.class);
        EventType threadDumpType = type(ThreadDumpProcessor.EVENT_NAME);
        EventType tlabType = type(TLABProcessor.NEW_TLAB_EVENT_NAME);

        when(threadDump.getEventType()).thenReturn(threadDumpType);
        when(tlab.getEventType()).thenReturn(tlabType);
    }

    private EventType type(String name) {
        EventType type = mock(EventType.class);
        when(type.getName()).thenReturn(name);
        return type;
    }

    @Test
    void testTlabEnabled() {
        Config config = mock(Config.class);
        when(config.getBoolean(Configuration.CONFIG_KEY_TLAB_ENABLED, false)).thenReturn(true);
        RelevantEvents relevantEvents = RelevantEvents.create(config);
        assertTrue(relevantEvents.isRelevant(threadDump));
        assertTrue(relevantEvents.isRelevant(tlab));
    }

    @Test
    void testTlabNotEnabled() {
        Config config = mock(Config.class);
        when(config.getBoolean(Configuration.CONFIG_KEY_TLAB_ENABLED, false)).thenReturn(false);
        RelevantEvents relevantEvents = RelevantEvents.create(config);
        assertTrue(relevantEvents.isRelevant(threadDump));
        assertFalse(relevantEvents.isRelevant(tlab));
    }

}