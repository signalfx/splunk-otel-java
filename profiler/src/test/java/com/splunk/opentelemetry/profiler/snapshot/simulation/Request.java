package com.splunk.opentelemetry.profiler.snapshot.simulation;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public class Request implements TextMapGetter<Request>, TextMapSetter<Request> {
  private final Map<String, String> headers = new ConcurrentHashMap<>();

  @Override
  public Iterable<String> keys(Request carrier) {
    return headers.keySet();
  }

  @Nullable
  @Override
  public String get(@Nullable Request carrier, String key) {
    return carrier.headers.get(key);
  }

  @Override
  public void set(@Nullable Request carrier, String key, String value) {
    carrier.headers.put(key, value);
  }

  @Override
  public String toString() {
    return "Request{" +
        "headers=" + headers +
        '}';
  }
}
