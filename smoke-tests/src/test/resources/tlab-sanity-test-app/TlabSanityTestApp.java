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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TlabSanityTestApp {

  private static final long TEST_DURATION_MS = TimeUnit.SECONDS.toMillis(20);

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    ThreadFactory namedThreadFactory =
        runnable -> {
          Thread t = Executors.defaultThreadFactory().newThread(runnable);
          t.setName("tlab-test-thread");
          return t;
        };

    // wait a bit for JFR to start recording
    TimeUnit.SECONDS.sleep(5);

    Executors.newSingleThreadExecutor(namedThreadFactory)
        .submit(
            () -> {
              instrumentedMethod();
              latch.countDown();
              return null;
            });

    System.out.println("Waiting for thread to finish ...");
    latch.await(30, TimeUnit.SECONDS);
    System.out.println("Done.");
  }

  private static void instrumentedMethod() throws InterruptedException {
    long startTime = System.currentTimeMillis();
    Object[] objects;

    System.out.println("Start creating objects ...");
    while (System.currentTimeMillis() - startTime < TEST_DURATION_MS) {
      objects = new Object[2_000_000];
      for (int i = 0; i < objects.length; ++i) {
        objects[i] = new Object();
      }

      System.out.println("Sleeping ...");
      TimeUnit.MILLISECONDS.sleep(500);
    }
  }
}
