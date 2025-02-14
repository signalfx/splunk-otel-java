package com.splunk.opentelemetry.profiler;

import java.util.Locale;

public enum InstrumentationSource {
  CONTINUOUS,
  SNAPSHOT;

  private final String value;

  InstrumentationSource() {
    this.value = name().toLowerCase(Locale.ROOT);
  }

  public String value() {
    return value;
  }
}
