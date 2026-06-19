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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

@AutoService({
  AutoConfigurationCustomizerProvider.class,
  DeclarativeConfigurationCustomizerProvider.class
})
public final class JfrContextStorageInitializer
    implements AutoConfigurationCustomizerProvider, DeclarativeConfigurationCustomizerProvider {

  @Override
  public void customize(@NonNull AutoConfigurationCustomizer autoConfiguration) {
    setupJfrContextStorage();
  }

  @Override
  public void customize(DeclarativeConfigurationCustomizer configurationCustomizer) {
    setupJfrContextStorage();
  }

  @Override
  public int order() {
    return Integer.MIN_VALUE;
  }

  private void setupJfrContextStorage() {
    if (JFR.getInstance().isAvailable()) {
      ProfilingSupervisor.setupJfrContextStorage();
    }
  }
}
