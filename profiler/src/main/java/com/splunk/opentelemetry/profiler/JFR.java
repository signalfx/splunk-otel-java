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

package com.splunk.opentelemetry.profiler;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Collections;
import java.util.logging.Logger;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.internal.Options;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.utility.JavaModule;

/** Abstraction around the JDK Flight Recorder subsystem. */
class JFR {
  private static final Logger logger = Logger.getLogger(JFR.class.getName());

  private static final JFR instance = new JFR();
  private static final boolean jfrAvailable = checkJfr();

  public static JFR getInstance() {
    return instance;
  }

  private static boolean checkJfr() {
    try {
      JFR.class.getClassLoader().loadClass("jdk.jfr.FlightRecorder");
      return FlightRecorder.isAvailable();
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public boolean isAvailable() {
    return jfrAvailable;
  }

  public Recording takeSnapshot() {
    return FlightRecorder.getFlightRecorder().takeSnapshot();
  }

  public RecordingFile openRecordingFile(Path path) {
    try {
      return new RecordingFile(path);
    } catch (IOException e) {
      throw new JfrException("Error opening recording file", e);
    }
  }

  public RecordedEvent readEvent(RecordingFile file, Path path) {
    try {
      return file.readEvent();
    } catch (IOException e) {
      throw new JfrException("Error reading events from " + path, e);
    }
  }

  public void setStackDepth(int stackDepth) {
    try {
      JfrAccess.init();
      int currentDepth = Options.getStackDepth();
      // we only increase stack depth
      if (currentDepth < stackDepth) {
        Options.setStackDepth(stackDepth);
      }
    } catch (Throwable throwable) {
      logger.log(WARNING, "Failed to set JFR stack depth to " + stackDepth, throwable);
    }
  }

  private static class JfrAccess {
    static {
      Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
      if (instrumentation != null && JavaModule.isSupported()) {
        // ensure that we have access to jdk.jfr.internal.Options.setStackDepth
        JavaModule currentModule = JavaModule.ofType(JFR.class);
        JavaModule flightRecorder = JavaModule.ofType(FlightRecorder.class);
        if (flightRecorder != null && flightRecorder.isNamed() && currentModule != null) {
          ClassInjector.UsingInstrumentation.redefineModule(
              instrumentation,
              flightRecorder,
              Collections.emptySet(),
              Collections.emptyMap(),
              Collections.singletonMap("jdk.jfr.internal", Collections.singleton(currentModule)),
              Collections.emptySet(),
              Collections.emptyMap());
        }
      }
    }

    static void init() {}
  }
}
