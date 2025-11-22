/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.incubator;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.entity.Entity;

/** Extension to {@link OpenTelemetry} with experimental APIs. */
public interface ExtendedOpenTelemetry extends OpenTelemetry {
  /** Returns the {@link ConfigProvider} for this {@link OpenTelemetry}. */
  default ConfigProvider getConfigProvider() {
    return ConfigProvider.noop();
  }

  /**
   * Creates a new {@link ExtendedOpenTelemetry} interface that will report signals against the
   * given entity.
   */
  ExtendedOpenTelemetry withEntity(Entity e);
}
