package com.splunk.opentelemetry.profiler.snapshot;

import java.security.SecureRandom;

class ProbabilisticTraceSelector implements TraceSelector {
    private final SecureRandom random = new SecureRandom();
    private final double selectionRate;

    public ProbabilisticTraceSelector(double selectionRate) {
        this.selectionRate = selectionRate;
    }

    @Override
    public boolean select() {
        return random.nextDouble() <= selectionRate;
    }
}
