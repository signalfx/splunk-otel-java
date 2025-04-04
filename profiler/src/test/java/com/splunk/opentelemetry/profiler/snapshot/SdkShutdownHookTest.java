package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class SdkShutdownHookTest {
    private final Closer closer = new Closer();
    private final SdkShutdownHook shutdownHook = new SdkShutdownHook(() -> closer);

    @Test
    void shutdownClosesAddedCloseable() {
        var thingToClose = new SuccessfulCloseable();

        closer.add(thingToClose);
        shutdownHook.shutdown();

        assertThat(thingToClose.closed).isTrue();
    }

    @Test
    void shutdownClosesMultipleAddedCloseable() {
        var one = new SuccessfulCloseable();
        var two = new SuccessfulCloseable();

        closer.add(one);
        closer.add(two);
        shutdownHook.shutdown();

        assertThat(one.closed).isTrue();
        assertThat(two.closed).isTrue();
    }

    @Test
    void shutdownReportsSuccessAllCloseablesCloseSuccessfully() {
        closer.add(new SuccessfulCloseable());
        closer.add(new SuccessfulCloseable());

        var result = shutdownHook.shutdown();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shutdownReportsFailureWhenCloseableFailsToClose() {
        closer.add(new ExceptionThrowingCloseable());

        var result = shutdownHook.shutdown();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void shutdownReportsFailureWhenAtLeaseCloseableFailsToClose() {
        closer.add(new SuccessfulCloseable());
        closer.add(new ExceptionThrowingCloseable());
        closer.add(new SuccessfulCloseable());

        var result = shutdownHook.shutdown();
        assertThat(result.isSuccess()).isFalse();
    }

    private static class SuccessfulCloseable implements Closeable {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class ExceptionThrowingCloseable implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException();
        }
    }
}
