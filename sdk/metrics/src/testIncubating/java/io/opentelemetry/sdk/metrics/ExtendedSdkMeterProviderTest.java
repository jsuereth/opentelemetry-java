/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.entity.internal.EntityUtil;
import io.opentelemetry.sdk.entity.internal.SdkEntity;
import io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.time.TestClock;
import org.junit.jupiter.api.Test;

class ExtendedSdkMeterProviderTest {
  private static final Resource RESOURCE = Resource.empty();
  private final TestClock testClock = TestClock.create();
  private final SdkMeterProviderBuilder sdkMeterProviderBuilder =
      SdkMeterProvider.builder().setClock(testClock).setResource(RESOURCE);

  @Test
  void withEntity_collectsMetricsOnNewResource() {
    InMemoryMetricReader sdkMeterReader = InMemoryMetricReader.create();
    SdkMeterProvider sdkMeterProvider =
        sdkMeterProviderBuilder.registerMetricReader(sdkMeterReader).build();
    SdkEntity entity =
        SdkEntity.builder("test")
            .setIdentity(Attributes.builder().put("test.id", 1).build())
            .build();
    SdkMeterProvider nested = SdkMeterProviderUtil.withEntity(sdkMeterProvider, entity);
    Meter nestedMeter = nested.get(ExtendedSdkMeterProviderTest.class.getName());
    LongCounter counter = nestedMeter.counterBuilder("testCounter").build();
    counter.add(10, Attributes.empty());

    assertThat(sdkMeterReader.collectAllMetrics())
        .hasSize(1)
        .satisfiesExactly(
            metric -> assertThat(EntityUtil.getEntities(metric.getResource())).contains(entity));
  }
}
