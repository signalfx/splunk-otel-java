package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

class Closer {
  private final List<Closeable> closeables = new ArrayList<>();

  void add(Closeable closeable) {
    closeables.add(closeable);
  }

  CompletableResultCode close() {
    List<CompletableResultCode> results = new ArrayList<>();
    for (Closeable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception e) {
        results.add(CompletableResultCode.ofExceptionalFailure(e));
      }
    }
    return CompletableResultCode.ofAll(results);
  }
}
