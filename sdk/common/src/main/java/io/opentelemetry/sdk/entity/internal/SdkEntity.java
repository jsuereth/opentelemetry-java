/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.entity.internal;

import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

/**
 * An entity that signals will be reported against.
 *
 * <p>This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
public final class SdkEntity {
  private final String entityType;
  @Nullable private final String schemaUrl;
  private final Attributes identity;
  private final Attributes description;

  SdkEntity(
      String entityType, @Nullable String schemaUrl, Attributes identity, Attributes description) {
    this.entityType = entityType;
    this.schemaUrl = schemaUrl;
    this.identity = identity;
    this.description = description;
  }

  /**
   * Returns the entity type string of this entity. Must not be null.
   *
   * @return the entity type.
   */
  public String getType() {
    return entityType;
  }

  /**
   * Returns a map of attributes that identify the entity.
   *
   * @return the entity identity.
   */
  public Attributes getIdentity() {
    return this.identity;
  }

  /**
   * Returns a map of attributes that describe the entity.
   *
   * @return the entity description.
   */
  public Attributes getDescription() {
    return this.description;
  }

  /**
   * Returns the URL of the OpenTelemetry schema used by this resource. May be null if this entity
   * does not abide by schema conventions (i.e. is custom).
   *
   * @return An OpenTelemetry schema URL.
   */
  @Nullable
  public String getSchemaUrl() {
    return this.schemaUrl;
  }

  /** Converts this entity to a builder. */
  public SdkEntityBuilder toBuilder() {
    return new SdkEntityBuilder(this);
  }

  /** Constructs a new builder of entity. */
  public static SdkEntityBuilder builder(String entityType) {
    return new SdkEntityBuilder(entityType);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + entityType.hashCode();
    result = prime * result + ((schemaUrl == null) ? 0 : schemaUrl.hashCode());
    result = prime * result + identity.hashCode();
    result = prime * result + description.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SdkEntity other = (SdkEntity) obj;
    if (!entityType.equals(other.entityType)) {
      return false;
    }
    if (schemaUrl == null) {
      if (other.schemaUrl != null) {
        return false;
      }
    } else if (!schemaUrl.equals(other.schemaUrl)) {
      return false;
    }
    if (!identity.equals(other.identity)) {
      return false;
    }
    if (!description.equals(other.description)) {
      return false;
    }
    return true;
  }
}
