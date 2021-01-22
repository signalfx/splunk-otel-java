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

package com.splunk.opentelemetry.helper;

import java.time.Duration;

public abstract class TargetWaitStrategy {
  public final Duration timeout;

  protected TargetWaitStrategy(Duration timeout) {
    this.timeout = timeout;
  }

  public static class Log extends TargetWaitStrategy {
    public final String regex;

    public Log(Duration timeout, String regex) {
      super(timeout);
      this.regex = regex;
    }
  }

  public static class Http extends TargetWaitStrategy {
    public final String path;

    public Http(Duration timeout, String path) {
      super(timeout);
      this.path = path;
    }
  }
}
