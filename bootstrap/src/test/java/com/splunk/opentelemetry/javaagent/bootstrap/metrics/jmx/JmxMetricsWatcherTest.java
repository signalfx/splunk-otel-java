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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxWatcher;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JmxMetricsWatcherTest {
  @Mock JmxWatcher jmxWatcher;
  @Mock MeterRegistry meterRegistry;
  @Mock MetersFactory metersFactory;
  @Mock MBeanServer mBeanServer;

  @InjectMocks JmxMetricsWatcher underTest;

  @Test
  void shouldAddMetersOnJmxRegister() throws Exception {
    // given
    var objectName = new ObjectName("com.splunk.test:type=Test");
    var meter1 = mock(Meter.class);
    var meter2 = mock(Meter.class);
    when(metersFactory.createMeters(mBeanServer, objectName)).thenReturn(List.of(meter1, meter2));

    // when
    underTest.onRegister(mBeanServer, objectName);

    // then
    assertThat(underTest.getAllMeters()).containsExactlyInAnyOrder(meter1, meter2);
  }

  @Test
  void shouldRemoveMetersOnJmxUnregister() throws Exception {
    // given
    var objectName1 = new ObjectName("com.splunk.test:type=Test,name=1");
    var objectName2 = new ObjectName("com.splunk.test:type=Test,name=2");
    var meter1 = mock(Meter.class);
    var meter2 = mock(Meter.class);
    when(metersFactory.createMeters(mBeanServer, objectName1)).thenReturn(List.of(meter1));
    when(metersFactory.createMeters(mBeanServer, objectName2)).thenReturn(List.of(meter2));

    underTest.onRegister(mBeanServer, objectName1);
    underTest.onRegister(mBeanServer, objectName2);

    assertThat(underTest.getAllMeters()).containsExactlyInAnyOrder(meter1, meter2);

    // when
    underTest.onUnregister(mBeanServer, objectName1);

    // then
    verify(meterRegistry).remove(meter1);

    assertThat(underTest.getAllMeters()).contains(meter2);
  }

  @Test
  void shouldRemoveAllMetersOnStop() throws Exception {
    // given
    var objectName1 = new ObjectName("com.splunk.test:type=Test,name=1");
    var objectName2 = new ObjectName("com.splunk.test:type=Test,name=2");
    var meter1 = mock(Meter.class);
    var meter2 = mock(Meter.class);
    when(metersFactory.createMeters(mBeanServer, objectName1)).thenReturn(List.of(meter1));
    when(metersFactory.createMeters(mBeanServer, objectName2)).thenReturn(List.of(meter2));

    underTest.onRegister(mBeanServer, objectName1);
    underTest.onRegister(mBeanServer, objectName2);

    assertThat(underTest.getAllMeters()).containsExactlyInAnyOrder(meter1, meter2);

    // when
    underTest.stop();

    // then
    verify(jmxWatcher).stop();
    verify(meterRegistry).remove(meter1);
    verify(meterRegistry).remove(meter2);

    assertThat(underTest.getAllMeters()).isEmpty();
  }
}
