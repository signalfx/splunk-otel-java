package com.splunk.opentelemetry.profiler.snapshot;

public enum Volume {
  OFF,
  HIGHEST;

  static Volume fromString(String value) {
    if (value == null) {
      return OFF;
    }

    try {
      return Volume.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return OFF;
    }
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
