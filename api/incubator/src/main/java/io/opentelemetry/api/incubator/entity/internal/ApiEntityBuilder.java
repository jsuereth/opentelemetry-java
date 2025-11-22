/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.incubator.entity.internal;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.entity.Entity;
import io.opentelemetry.api.incubator.entity.EntityBuilder;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class ApiEntityBuilder implements EntityBuilder {
  private final String entityType;
  @Nullable private String schemaUrl;
  private Attributes identity;
  private Attributes description;

  public ApiEntityBuilder(String entityType) {
    this.entityType = entityType;
    this.identity = Attributes.empty();
    this.description = Attributes.empty();
  }

  ApiEntityBuilder(ApiEntity entity) {
    this.entityType = entity.getType();
    this.schemaUrl = entity.getSchemaUrl();
    this.identity = entity.getIdentity();
    this.description = entity.getDescription();
  }

  @Override
  public EntityBuilder setSchemaUrl(String schemaUrl) {
    this.schemaUrl = schemaUrl;
    return this;
  }

  @Override
  public EntityBuilder setIdentity(Attributes identity) {
    this.identity = identity;
    return this;
  }

  @Override
  public EntityBuilder setDescription(Attributes description) {
    this.description = description;
    return this;
  }

  @Override
  public Entity build() {
    // TODO - assertions around safe entity builds.
    // TODO - identity is not empty.
    return new ApiEntity(entityType, schemaUrl, identity, description);
  }
}
