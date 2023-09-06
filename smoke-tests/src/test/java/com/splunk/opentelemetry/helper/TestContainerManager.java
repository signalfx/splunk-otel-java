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

package com.splunk.opentelemetry.helper;

import org.testcontainers.containers.GenericContainer;

public interface TestContainerManager {
  String TARGET_AGENT_FILENAME = "opentelemetry-javaagent.jar";

  void startEnvironment();

  void stopEnvironment();

  void startCollector();

  void stopCollector();

  boolean isImageCompatible(TestImage image);

  boolean isImagePresent(TestImage image);

  int getBackendMappedPort();

  int getHecBackendMappedPort();

  int getTargetMappedPort(int originalPort);

  void startTarget(TargetContainerBuilder builder);

  void stopTarget();

  GenericContainer<?> getTargetContainer();

  GenericContainer<?> newContainer(TestImage image);
}
