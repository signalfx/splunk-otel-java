/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
