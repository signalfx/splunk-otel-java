package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class SdkShutdownHookTest {
    private final ClosingObserver observer = new ClosingObserver();
    private final SdkShutdownHook shutdownHook = new SdkShutdownHook();

    @Test
    void shutdownStackTraceSampling() {
        try {
          StackTraceSampler.SUPPLIER.configure(observer);
          shutdownHook.shutdown();
          assertThat(observer.isClosed).isTrue();
        } finally {
          StackTraceSampler.SUPPLIER.reset();
        }
    }

    @Test
    void shutdownStagingArea() {
        try {
            StagingArea.SUPPLIER.configure(observer);
            shutdownHook.shutdown();
            assertThat(observer.isClosed).isTrue();
        } finally {
          StagingArea.SUPPLIER.reset();
        }
    }

    @Test
    void shutdownStackTraceExporting() {
        try {
            StackTraceExporter.SUPPLIER.configure(observer);
            shutdownHook.shutdown();
            assertThat(observer.isClosed).isTrue();
        } finally {
          StackTraceExporter.SUPPLIER.reset();
        }
    }

    private static class ClosingObserver implements StackTraceSampler, StagingArea, StackTraceExporter {
        private boolean isClosed = false;

        @Override
        public void close() {
            isClosed = true;
        }

        @Override public void start(SpanContext spanContext) {}
        @Override public void stop(SpanContext spanContext) {}
        @Override public void stage(String traceId, StackTrace stackTrace) {}
        @Override public void empty(String traceId) {}
        @Override public void export(List<StackTrace> stackTraces) {}
    }
}
