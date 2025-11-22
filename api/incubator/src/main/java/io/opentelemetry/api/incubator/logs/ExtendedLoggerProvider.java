/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.incubator.logs;

import io.opentelemetry.api.incubator.entity.Entity;
import io.opentelemetry.api.logs.LoggerProvider;

/** Extended {@link LoggerProvider} with experimental APIs. */
public interface ExtendedLoggerProvider extends LoggerProvider {
  /**
   * Creates a new {@link ExtendedLoggerProvider} interface that will report signals against the
   * given entity.
   */
  ExtendedLoggerProvider withEntity(Entity entity);
}
