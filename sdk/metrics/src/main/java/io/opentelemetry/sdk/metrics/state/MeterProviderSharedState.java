/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.MeasurementProcessor;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.concurrent.Immutable;

@AutoValue
@Immutable
public abstract class MeterProviderSharedState {
  public static MeterProviderSharedState create(
      Clock clock, Resource resource, MeasurementProcessor processor) {
    return new AutoValue_MeterProviderSharedState(clock, resource, processor, clock.now());
  }

  public abstract Clock getClock();

  public abstract Resource getResource();

  public abstract MeasurementProcessor getMeasurementProcessor();

  public abstract long getStartEpochNanos();
}
