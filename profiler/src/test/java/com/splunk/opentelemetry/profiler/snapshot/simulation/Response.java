package com.splunk.opentelemetry.profiler.snapshot.simulation;

public class Response {
  public static Response from(Request request) {
    return new Response();
  }

  private Response() {}
}
