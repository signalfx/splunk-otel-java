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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TargetContainerBuilder {
  public final String targetImageName;
  public String agentPath = null;
  public String jvmArgsEnvVarName = "JAVA_TOOL_OPTIONS";
  public Map<String, String> extraEnv = Collections.emptyMap();
  public List<ResourceMapping> extraResources = Collections.emptyList();
  public TargetWaitStrategy waitStrategy;
  public int targetPort = 8080;
  public List<String> networkAliases = Collections.emptyList();
  public List<String> entrypoint = null;
  public List<String> command = null;
  public List<FileSystemBind> fileSystemBinds = Collections.emptyList();
  public boolean useDefaultAgentConfiguration = true;

  public TargetContainerBuilder(String targetImageName) {
    this.targetImageName = targetImageName;
  }

  public TargetContainerBuilder withAgentPath(String agentPath) {
    this.agentPath = agentPath;
    return this;
  }

  public TargetContainerBuilder withJvmArgsEnvVarName(String jvmArgsEnvVarName) {
    this.jvmArgsEnvVarName = jvmArgsEnvVarName;
    return this;
  }

  public TargetContainerBuilder withExtraEnv(Map<String, String> extraEnv) {
    this.extraEnv = extraEnv;
    return this;
  }

  public TargetContainerBuilder withExtraResources(ResourceMapping... extraResources) {
    return withExtraResources(Arrays.asList(extraResources));
  }

  public TargetContainerBuilder withExtraResources(List<ResourceMapping> extraResources) {
    this.extraResources = extraResources;
    return this;
  }

  public TargetContainerBuilder withWaitStrategy(TargetWaitStrategy waitStrategy) {
    this.waitStrategy = waitStrategy;
    return this;
  }

  public TargetContainerBuilder withTargetPort(int targetPort) {
    this.targetPort = targetPort;
    return this;
  }

  public TargetContainerBuilder withNetworkAliases(String... networkAliases) {
    return withNetworkAliases(Arrays.asList(networkAliases));
  }

  public TargetContainerBuilder withNetworkAliases(List<String> networkAliases) {
    this.networkAliases = networkAliases;
    return this;
  }

  public TargetContainerBuilder withEntrypoint(String... entrypoint) {
    return withEntrypoint(Arrays.asList(entrypoint));
  }

  public TargetContainerBuilder withEntrypoint(List<String> entrypoint) {
    this.entrypoint = entrypoint;
    return this;
  }

  public TargetContainerBuilder withCommand(String... command) {
    return withCommand(Arrays.asList(command));
  }

  public TargetContainerBuilder withCommand(List<String> command) {
    this.command = command;
    return this;
  }

  public TargetContainerBuilder withFileSystemBinds(FileSystemBind... fileSystemBinds) {
    return withFileSystemBinds(Arrays.asList(fileSystemBinds));
  }

  public TargetContainerBuilder withFileSystemBinds(List<FileSystemBind> fileSystemBinds) {
    this.fileSystemBinds = fileSystemBinds;
    return this;
  }

  public TargetContainerBuilder withUseDefaultAgentConfiguration(
      boolean useDefaultAgentConfiguration) {
    this.useDefaultAgentConfiguration = useDefaultAgentConfiguration;
    return this;
  }

  public static class FileSystemBind {
    public final String hostPath;
    public final String containerPath;
    public final boolean isReadOnly;

    public FileSystemBind(String hostPath, String containerPath, boolean isReadOnly) {
      this.hostPath = hostPath;
      this.containerPath = containerPath;
      this.isReadOnly = isReadOnly;
    }
  }
}
