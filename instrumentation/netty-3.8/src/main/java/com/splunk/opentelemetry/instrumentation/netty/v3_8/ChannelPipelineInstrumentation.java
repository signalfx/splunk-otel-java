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

package com.splunk.opentelemetry.instrumentation.netty.v3_8;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.CallDepth;
import io.opentelemetry.javaagent.instrumentation.netty.v3_8.ChannelTraceContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpServerCodec;

public class ChannelPipelineInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.jboss.netty.channel.ChannelPipeline");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.jboss.netty.channel.ChannelPipeline"));
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    typeTransformer.applyAdviceToMethod(
        isMethod()
            .and(nameStartsWith("add"))
            .and(takesArgument(1, named("org.jboss.netty.channel.ChannelHandler"))),
        this.getClass().getName() + "$ChannelPipelineAdd2ArgsAdvice");
    typeTransformer.applyAdviceToMethod(
        isMethod()
            .and(nameStartsWith("add"))
            .and(takesArgument(2, named("org.jboss.netty.channel.ChannelHandler"))),
        this.getClass().getName() + "$ChannelPipelineAdd3ArgsAdvice");
  }

  @SuppressWarnings("unused")
  public static class ChannelPipelineAdd2ArgsAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void checkDepth(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(1) ChannelHandler handler,
        @Advice.Local("splunkCallDepth") CallDepth callDepth) {
      ChannelPipelineUtil.removeDuplicatesAndIncrementDepth(pipeline, handler);

      // CallDepth does not allow just getting the depth value, so to avoid interfering with the
      // upstream netty implementation we do the same count but with our class
      callDepth = CallDepth.forClass(ServerTimingHandler.class);
      callDepth.getAndIncrement();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addHandler(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(1) ChannelHandler handler,
        @Advice.Local("splunkCallDepth") CallDepth callDepth) {
      if (callDepth.decrementAndGet() > 0) {
        return;
      }

      VirtualField<Channel, ChannelTraceContext> channelTraceContextField =
          VirtualField.find(Channel.class, ChannelTraceContext.class);

      ChannelPipelineUtil.addServerTimingHandler(pipeline, handler, channelTraceContextField);
    }
  }

  @SuppressWarnings("unused")
  public static class ChannelPipelineAdd3ArgsAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void checkDepth(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(2) ChannelHandler handler,
        @Advice.Local("splunkCallDepth") CallDepth callDepth) {
      ChannelPipelineUtil.removeDuplicatesAndIncrementDepth(pipeline, handler);

      // CallDepth does not allow just getting the depth value, so to avoid interfering with the
      // upstream netty implementation we do the same count but with our class
      callDepth = CallDepth.forClass(ServerTimingHandler.class);
      callDepth.getAndIncrement();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addHandler(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(2) ChannelHandler handler,
        @Advice.Local("splunkCallDepth") CallDepth callDepth) {
      if (callDepth.decrementAndGet() > 0) {
        return;
      }

      VirtualField<Channel, ChannelTraceContext> channelTraceContextField =
          VirtualField.find(Channel.class, ChannelTraceContext.class);

      ChannelPipelineUtil.addServerTimingHandler(pipeline, handler, channelTraceContextField);
    }
  }

  public static final class ChannelPipelineUtil {
    public static void removeDuplicatesAndIncrementDepth(
        ChannelPipeline pipeline, ChannelHandler handler) {
      // Pipelines are created once as a factory and then copied multiple times using the same add
      // methods as we are hooking. If our handler has already been added we need to remove it so we
      // don't end up with duplicates (this throws an exception)
      if (pipeline.get(handler.getClass().getName()) != null) {
        pipeline.remove(handler.getClass().getName());
      }
    }

    public static void addServerTimingHandler(
        ChannelPipeline pipeline,
        ChannelHandler handler,
        VirtualField<Channel, ChannelTraceContext> channelTraceContextField) {
      if (handler instanceof HttpServerCodec || handler instanceof HttpResponseEncoder) {
        pipeline.addLast(
            ServerTimingHandler.class.getName(), new ServerTimingHandler(channelTraceContextField));
      }
    }

    private ChannelPipelineUtil() {}
  }
}
