/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.incubator.entity;

import io.opentelemetry.api.incubator.entity.internal.ApiEntityBuilder;

/** An instance of an Entity. */
public interface Entity {
  /** Constructs a new builder for creating Entities. */
  static EntityBuilder builder(String entityType) {
    return new ApiEntityBuilder(entityType);
  }

  /** Converts this entity to a builder. */
  EntityBuilder toBuilder();
}
