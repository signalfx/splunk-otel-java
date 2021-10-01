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

import static java.lang.Integer.parseInt;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jdk.jfr.Recording;

/**
 * Just runs for a while to allow some jfr file data to be created. args[0] == filename, args[1] =
 * length of time to run (in seconds)
 */
public class JfrFileMaker {

  private static final int NUM_THREADS = 40;
  private static final ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);

  public static void main(String[] args) throws Exception {
    String jfrFilename = args[0];
    int runSeconds = parseInt(args[1]);
    startThreads(runSeconds);
    Recording recording = new Recording();
    Map<String, String> settings = new HashMap<>();
    settings.put("jdk.ThreadDump#enabled", "true");
    settings.put("jdk.ThreadDump#period", "100 ms");
    recording.setSettings(settings);
    recording.setToDisk(true);
    recording.setDuration(null);
    System.out.println("Starting JFR recording for " + runSeconds);
    recording.start();
    TimeUnit.SECONDS.sleep(runSeconds);
    System.out.println("Dumping JFR contents to " + jfrFilename);
    recording.dump(Path.of(jfrFilename));
    recording.stop();
    pool.shutdownNow();
  }

  private static void startThreads(int runSeconds) {
    for (int i = 0; i < NUM_THREADS; i++) {
      pool.submit(
          () -> {
            try {
              TimeUnit.SECONDS.sleep(runSeconds);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          });
    }
  }
}
