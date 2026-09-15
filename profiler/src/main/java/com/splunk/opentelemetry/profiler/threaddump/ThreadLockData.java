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

package com.splunk.opentelemetry.profiler.threaddump;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadLockData {
  private String waitingOn;
  private String lockOwner;
  private final List<String> lockedMonitors = new ArrayList<>();
  private final List<String> lockedSynchronizers = new ArrayList<>();

  public ThreadLockData() {}

  public String getWaitingOn() {
    return waitingOn;
  }

  public String getLockOwner() {
    return lockOwner;
  }

  public List<String> getLockedMonitors() {
    return Collections.unmodifiableList(lockedMonitors);
  }

  public List<String> getLockedSynchronizers() {
    return Collections.unmodifiableList(lockedSynchronizers);
  }

  public void setWaitingOn(String waitingOn) {
    this.waitingOn = waitingOn;
  }

  public void setLockOwner(String lockOwner) {
    this.lockOwner = lockOwner;
  }

  public void addLockedMonitor(String lockedMonitor) {
    lockedMonitors.add(lockedMonitor);
  }

  public void addLockedSynchronizer(String lockedSynchronizer) {
    lockedSynchronizers.add(lockedSynchronizer);
  }
}
