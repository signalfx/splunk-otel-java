package com.splunk.opentelemetry.profiler;

import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;

public class LogEntryCommonAttributes {

    private final EventPeriods periods;

    public LogEntryCommonAttributes(EventPeriods periods) {
        this.periods = periods;
    }

    Attributes build(StackToSpanLinkage linkedStack) {
        return build(linkedStack.getSourceEvent());
    }

    Attributes build(RecordedEvent sourceEvent) {
        String eventName = sourceEvent.getEventType().getName();
        Duration eventPeriod = periods.getDuration(eventName);

        // Note: It is currently believed that the span id and trace id on the LogRecord itself
        // do not get ingested correctly. Placing them here as attributes is a temporary workaround
        // until the collector/ingest can be remedied.

        AttributesBuilder builder =
            Attributes.builder().put(SOURCE_TYPE, LogEntryCreator.PROFILING_SOURCE).put(SOURCE_EVENT_NAME, eventName);

        if (!EventPeriods.UNKNOWN.equals(eventPeriod)) {
          builder.put(SOURCE_EVENT_PERIOD, eventPeriod.toMillis());
        }
        return builder.build();
    }
}
