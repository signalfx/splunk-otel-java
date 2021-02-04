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

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.opentelemetry.javaagent.instrumentation.netty.v3_8.ChannelTraceContext;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoService(InstrumentationModule.class)
public class NettyInstrumentationModule extends InstrumentationModule {
  public NettyInstrumentationModule() {
    super("netty", "netty-3.8");
  }

  @Override
  protected String[] additionalHelperClassNames() {
    return new String[] {
      ServerTimingHeader.class.getName(),
      getClass().getPackage().getName() + ".ServerTimingHandler",
      getClass().getPackage().getName() + ".ServerTimingHandler$HeadersSetter",
      getClass().getPackage().getName()
          + ".NettyChannelPipelineInstrumentation$ChannelPipelineUtil",
    };
  }

  // run after the upstream netty instrumentation
  @Override
  public int getOrder() {
    return 1;
  }

  // enable the instrumentation only if the server-timing header flag is on
  @Override
  protected boolean defaultEnabled() {
    return super.defaultEnabled() && ServerTimingHeader.shouldEmitServerTimingHeader();
  }

  @Override
  public Map<String, String> contextStore() {
    return Collections.singletonMap(
        "org.jboss.netty.channel.Channel", ChannelTraceContext.class.getName());
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new NettyChannelPipelineInstrumentation());
  }
}
