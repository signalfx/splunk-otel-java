package com.splunk.opentelemetry.profiler.snapshot.simulation;

import java.time.Duration;
import java.util.function.Function;

public class Delay implements Function<Request, Response> {
  public static Delay of(Duration duration) {
    return new Delay(duration);
  }

  private Delay(Duration duration) {
    this.duration = duration;
  }

  private final Duration duration;

  @Override
  public Response apply(Request request) {
    try {
      Thread.sleep(duration.toMillis());
      return Response.from(request);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
