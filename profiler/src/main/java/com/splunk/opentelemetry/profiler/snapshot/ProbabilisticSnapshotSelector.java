package com.splunk.opentelemetry.profiler.snapshot;

import java.security.SecureRandom;

class ProbabilisticSnapshotSelector implements SnapshotSelector {
    private final SecureRandom random = new SecureRandom();
    private final double selectionRate;

    public ProbabilisticSnapshotSelector(double selectionRate) {
        this.selectionRate = selectionRate;
    }

    @Override
    public boolean select() {
        return random.nextDouble() <= selectionRate;
    }
}
