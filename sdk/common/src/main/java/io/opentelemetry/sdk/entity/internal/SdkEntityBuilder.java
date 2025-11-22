/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.entity.internal;

import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

/**
 * An builder of {@link SdkEntity}
 *
 * <p>This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
public final class SdkEntityBuilder {
  private final String entityType;
  @Nullable private String schemaUrl;
  private Attributes identity;
  private Attributes description;

  SdkEntityBuilder(String entityType) {
    this.entityType = entityType;
    this.identity = Attributes.empty();
    this.description = Attributes.empty();
  }

  SdkEntityBuilder(SdkEntity entity) {
    this.entityType = entity.getType();
    this.schemaUrl = entity.getSchemaUrl();
    this.identity = entity.getIdentity();
    this.description = entity.getDescription();
  }

  /** Sets the schema_url of this entity. */
  public SdkEntityBuilder setSchemaUrl(String schemaUrl) {
    this.schemaUrl = schemaUrl;
    return this;
  }

  /** Sets the identity of this entity. */
  public SdkEntityBuilder setIdentity(Attributes identity) {
    this.identity = identity;
    return this;
  }

  /** Sets the description of this entity. */
  public SdkEntityBuilder setDescription(Attributes description) {
    this.description = description;
    return this;
  }

  /** Returns the complete entity. */
  public SdkEntity build() {
    // TODO - assertions around safe entity builds.
    // TODO - identity is not empty.
    return new SdkEntity(entityType, schemaUrl, identity, description);
  }
}
