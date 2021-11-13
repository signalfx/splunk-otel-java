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

import java.io.IOException;
import java.nio.file.Path;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/** Abstraction around the JDK Flight Recorder subsystem. */
class JFR {

  public static final JFR instance = new JFR();
  private static final boolean jfrAvailable = checkJfr();

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
}
