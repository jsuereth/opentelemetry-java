/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.incubator.trace;

import io.opentelemetry.api.incubator.entity.Entity;
import io.opentelemetry.api.trace.TracerProvider;

/** Extended {@link TracerProvider} with experimental APIs. */
public interface ExtendedTracerProvider extends TracerProvider {
  /**
   * Creates a new {@link ExtendedTracerProvider} interface that will report signals against the
   * given entity.
   */
  ExtendedTracerProvider withEntity(Entity entity);
}
