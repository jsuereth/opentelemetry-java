/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.extension.incubator.entity.internal;

import io.opentelemetry.api.incubator.entity.Entity;
import io.opentelemetry.api.incubator.entity.internal.ApiEntity;
import io.opentelemetry.sdk.entity.internal.SdkEntity;
import io.opentelemetry.sdk.entity.internal.SdkEntityBuilder;

/**
 * This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
public final class ExtendedEntityUtil {
  private ExtendedEntityUtil() {}

  /** Convert between the incubator API entity and the internal-implementation SDK entity. */
  public static SdkEntity convertEntity(Entity entity) {
    ApiEntity api = (ApiEntity) entity;
    SdkEntityBuilder builder = SdkEntity.builder(api.getType());
    if (api.getSchemaUrl() != null) {
      builder.setSchemaUrl(api.getSchemaUrl());
    }
    builder.setIdentity(api.getIdentity());
    builder.setDescription(api.getDescription());
    return builder.build();
  }
}
