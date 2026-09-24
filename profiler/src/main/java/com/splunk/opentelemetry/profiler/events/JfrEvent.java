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

package com.splunk.opentelemetry.profiler.events;

/**
 * Test-only abstraction for the JFR event lifecycle methods used by the profiler.
 *
 * <p>{@link ContextAttached} extends {@link jdk.jfr.Event}. At runtime, JFR instruments concrete
 * event subclasses by generating implementations of lifecycle methods such as {@code begin()},
 * {@code commit()}, and {@code shouldCommit()}. On Java 21, this instrumentation interferes with
 * Mockito's inline transformation of the event subclass. Calls can consequently reach a
 * JFR-generated implementation instead of Mockito's mock handler, causing Mockito to report a
 * {@code MissingMethodInvocationException} when a test tries to stub the method.
 *
 * <p>Production JFR events implement this interface, while the code under test depends on the
 * interface rather than directly on the concrete JFR event. Mockito can then mock the lifecycle
 * contract without transforming a JFR event subclass.
 *
 * @see ContextAttached
 * @see jdk.jfr.Event
 */
public interface JfrEvent {
  void begin();

  boolean shouldCommit();

  void commit();
}
