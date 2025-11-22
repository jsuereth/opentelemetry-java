/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.incubator.metrics;

import io.opentelemetry.api.incubator.entity.Entity;
import io.opentelemetry.api.metrics.MeterProvider;

/** Extended {@link MeterProvider} with experimental APIs. */
public interface ExtendedMeterProvider extends MeterProvider {
  /**
   * Creates a new {@link ExtendedMeterProvider} interface that will report signals against the
   * given entity.
   */
  ExtendedMeterProvider withEntity(Entity entity);
}
