package com.splunk.opentelemetry.profiler.snapshot;

import java.util.function.Supplier;

class CloserProvider implements Supplier<Closer> {
    static final CloserProvider INSTANCE = new CloserProvider();

    private Closer closer = Closer.NOOP;

    @Override
    public Closer get() {
        return closer;
    }

    void configure(Closer closer) {
        if (closer != null) {
            this.closer = closer;
        }
    }

    void reset() {
        closer = Closer.NOOP;
    }

    private CloserProvider(){}
}
