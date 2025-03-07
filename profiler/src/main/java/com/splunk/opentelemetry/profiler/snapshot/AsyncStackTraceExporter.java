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

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.logs.Logger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

class AsyncStackTraceExporter implements StackTraceExporter {
  private final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private final Logger logger;
  private final BiFunction<Logger, List<StackTrace>, Runnable> workerFactory;

  AsyncStackTraceExporter(
      Logger logger, BiFunction<Logger, List<StackTrace>, Runnable> workerFactory) {
    this.logger = logger;
    this.workerFactory = workerFactory;
  }

  @Override
  public void export(List<StackTrace> stackTraces) {
    executor.submit(workerFactory.apply(logger, stackTraces));
  }
}
