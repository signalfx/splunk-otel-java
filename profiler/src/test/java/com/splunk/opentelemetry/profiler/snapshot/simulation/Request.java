package com.splunk.opentelemetry.profiler.snapshot.simulation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Request {
  public static Request newRequest() {
    return new Request();
  }

  public final Map<String, String> headers = new ConcurrentHashMap<>();

  public Request copy() {
    return new Request();
  }

  @Override
  public String toString() {
    return "Request{" +
        "headers=" + headers +
        '}';
  }
}
