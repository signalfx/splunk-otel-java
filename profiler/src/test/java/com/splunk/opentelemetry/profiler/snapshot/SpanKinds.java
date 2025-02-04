package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanKind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class SpanKinds {
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @EnumSource(value = SpanKind.class, mode = Mode.INCLUDE, names = {"SERVER", "CONSUMER"})
  @interface Entry {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @EnumSource(value = SpanKind.class, mode = Mode.EXCLUDE, names = {"SERVER", "CONSUMER"})
  @interface NonEntry {}

  private SpanKinds(){}
}
