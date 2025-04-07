package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SdkShutdownHookTest {
    private final ClosingObserver observer = new ClosingObserver();
    private final SdkShutdownHook shutdownHook = new SdkShutdownHook();

    @Test
    void shutdownStackTraceSampling() {
        try {
            StackTraceSamplerProvider.INSTANCE.configure(observer);
            shutdownHook.shutdown();
            assertThat(observer.isClosed).isTrue();
        } finally {
            StackTraceSamplerProvider.INSTANCE.reset();
        }
    }

    @Test
    void shutdownStagingArea() {
        try {
            StagingAreaProvider.INSTANCE.configure(observer);
            shutdownHook.shutdown();
            assertThat(observer.isClosed).isTrue();
        } finally {
            StackTraceSamplerProvider.INSTANCE.reset();
        }
    }

    @Test
    void shutdownStackTraceExporting() {
        try {
            StackTraceExporterProvider.INSTANCE.configure(observer);
            shutdownHook.shutdown();
            assertThat(observer.isClosed).isTrue();
        } finally {
            StackTraceSamplerProvider.INSTANCE.reset();
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
