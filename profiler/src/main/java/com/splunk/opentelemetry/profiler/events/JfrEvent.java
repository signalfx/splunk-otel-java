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
 * <p>{@link ContextAttached} extends {@link jdk.jfr.Event}, whose lifecycle methods are final. When
 * tests run on Java 21, Mockito's inline mock maker does not reliably intercept those inherited JFR
 * methods. As a result, the real JFR method is invoked and Mockito reports a
 * {@code MissingMethodInvocationException} when the test tries to stub it.
 *
 * <p>Production JFR events implement this interface, while the code under test depends on the
 * interface rather than directly on the concrete JFR event. Mockito can then mock these ordinary,
 * non-final interface methods without instrumenting {@code jdk.jfr.Event}.
 *
 * @see ContextAttached
 * @see jdk.jfr.Event
 */
public interface JfrEvent {
  void begin();

  boolean shouldCommit();

  void commit();
}
