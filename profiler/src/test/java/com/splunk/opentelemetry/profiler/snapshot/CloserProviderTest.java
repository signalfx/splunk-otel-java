package com.splunk.opentelemetry.profiler.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloserProviderTest {
    private final CloserProvider provider = CloserProvider.INSTANCE;

    @AfterEach
    void tearDown() {
        provider.reset();
    }

    @Test
    void provideNoopCloserWhenNotConfigured() {
        assertSame(Closer.NOOP, provider.get());
    }

    @Test
    void providedConfiguredCloser() {
        var closer = new Closer();
        provider.configure(closer);
        assertSame(closer, provider.get());
    }

    @Test
    void canResetConfiguredCloser() {
        provider.reset();
        assertSame(Closer.NOOP, provider.get());
    }
}
