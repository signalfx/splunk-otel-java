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

package com.splunk.opentelemetry.middleware;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.MiddlewareHolder;
import io.opentelemetry.javaagent.spi.BootstrapPackagesProvider;
import java.util.Collections;
import java.util.List;

@AutoService(BootstrapPackagesProvider.class)
public class MiddlewareBootstrapPackagesProvider implements BootstrapPackagesProvider {

  @Override
  public List<String> getPackagePrefixes() {
    return Collections.singletonList(MiddlewareHolder.class.getPackage().getName());
  }
}
