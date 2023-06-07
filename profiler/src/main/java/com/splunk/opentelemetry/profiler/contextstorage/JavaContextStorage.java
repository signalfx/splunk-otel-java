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

package com.splunk.opentelemetry.profiler.contextstorage;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.instrumentation.api.internal.cache.Cache;

// active context tracking for java profiler
public class JavaContextStorage extends AbstractContextStorage {

  public static final Cache<Thread, SpanContext> activeContext = Cache.weak();
  private static final Guard NOP = () -> {};
  private static final BlockingGuard GUARD = new BlockingGuard();
  private static volatile Guard guard = NOP;

  public JavaContextStorage(ContextStorage delegate) {
    super(delegate);
  }

  public static void block() {
    guard = GUARD;
  }

  public static void unblock() {
    guard = NOP;
    GUARD.release();
  }

  @Override
  protected void activateSpan(Span span) {
    // when taking thread dump we block all thread that attempt to modify the active contexts
    guard.stop();

    SpanContext context = span.getSpanContext();
    if (context.isValid()) {
      activeContext.put(Thread.currentThread(), context);
    } else {
      activeContext.remove(Thread.currentThread());
    }
  }

  private interface Guard {
    void stop();
  }

  private static class BlockingGuard implements Guard {

    @Override
    public synchronized void stop() {
      try {
        while (guard == GUARD) {
          wait();
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }

    synchronized void release() {
      notifyAll();
    }
  }
}
