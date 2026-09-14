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

package com.splunk.opentelemetry.profiler.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ThreadLockData {
  private final String waitingOn;
  private final String lockOwner;
  private final List<String> lockedMonitors;
  private final List<String> lockedSynchronizers;

  private ThreadLockData(Builder builder) {
    this.waitingOn = builder.waitingOn;
    this.lockOwner = builder.lockOwner;
    this.lockedMonitors = immutableCopy(builder.lockedMonitors);
    this.lockedSynchronizers = immutableCopy(builder.lockedSynchronizers);
  }

  private static <T> List<T> immutableCopy(List<T> source) {
    return Collections.unmodifiableList(new ArrayList<>(source));
  }

  static Builder builder() {
    return new Builder();
  }

  String getWaitingOn() {
    return waitingOn;
  }

  String getLockOwner() {
    return lockOwner;
  }

  List<String> getLockedMonitors() {
    return lockedMonitors;
  }

  List<String> getLockedSynchronizers() {
    return lockedSynchronizers;
  }

  static class Builder {
    private String waitingOn;
    private String lockOwner;
    private final List<String> lockedMonitors = new ArrayList<>();
    private final List<String> lockedSynchronizers = new ArrayList<>();

    Builder setWaitingOn(String waitingOn) {
      this.waitingOn = waitingOn;
      return this;
    }

    Builder setLockOwner(String lockOwner) {
      this.lockOwner = lockOwner;
      return this;
    }

    Builder addLockedMonitor(String lockedMonitor) {
      lockedMonitors.add(lockedMonitor);
      return this;
    }

    Builder addLockedSynchronizer(String lockedSynchronizer) {
      lockedSynchronizers.add(lockedSynchronizer);
      return this;
    }

    ThreadLockData build() {
      return new ThreadLockData(this);
    }
  }
}
