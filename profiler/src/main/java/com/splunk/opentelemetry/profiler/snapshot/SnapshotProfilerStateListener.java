package com.splunk.opentelemetry.profiler.snapshot;

public interface SnapshotProfilerStateListener {
  void onSnapshotProfilerStateChanged(boolean enabled);
}
