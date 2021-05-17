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

package com.splunk.opentelemetry.netty.v3_8;

import static io.opentelemetry.javaagent.extension.matcher.ClassLoaderMatcher.hasClassesNamed;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.instrumentation.netty.v3_8.ChannelTraceContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class NettyInstrumentationModule extends InstrumentationModule {
  public NettyInstrumentationModule() {
    super("netty", "netty-3.8");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("org.jboss.netty.channel.ChannelPipeline");
  }

  @Override
  public String[] getMuzzleHelperClassNames() {
    return new String[] {
      ServerTimingHeader.class.getName(),
      getClass().getPackage().getName() + ".ServerTimingHandler",
      getClass().getPackage().getName() + ".ServerTimingHandler$HeadersSetter",
      getClass().getPackage().getName() + ".ChannelPipelineInstrumentation$ChannelPipelineUtil",
    };
  }

  // run after the upstream netty instrumentation
  @Override
  public int order() {
    return 1;
  }

  // enable the instrumentation only if the server-timing header flag is on
  @Override
  protected boolean defaultEnabled() {
    return super.defaultEnabled() && ServerTimingHeader.shouldEmitServerTimingHeader();
  }

  // we don't have muzzle in our distro
  @Override
  public Map<String, String> getMuzzleContextStoreClasses() {
    return Collections.singletonMap(
        "org.jboss.netty.channel.Channel", ChannelTraceContext.class.getName());
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new ChannelPipelineInstrumentation());
  }
}
