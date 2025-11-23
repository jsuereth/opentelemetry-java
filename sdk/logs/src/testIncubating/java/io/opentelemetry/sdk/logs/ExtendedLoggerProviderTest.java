/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logs;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.entity.internal.EntityUtil;
import io.opentelemetry.sdk.entity.internal.SdkEntity;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.logs.internal.SdkLoggerProviderUtil;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import org.junit.jupiter.api.Test;

class ExtendedLoggerProviderTest {
  private final InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
  private final SdkLoggerProviderBuilder loggerProviderBuilder =
      SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter));

  @Test
  void withEntity_exportsWithNewResource() {
    SdkLoggerProvider loggerProvider = loggerProviderBuilder.build();
    SdkEntity entity =
        SdkEntity.builder("test")
            .setIdentity(Attributes.builder().put("test.id", 1).build())
            .build();
    SdkLoggerProvider nestedProvider = SdkLoggerProviderUtil.withEntity(loggerProvider, entity);
    // Export a normal log
    loggerProvider.get("scope").logRecordBuilder().setEventName("test").emit();
    // Export a nested log
    nestedProvider.get("scope").logRecordBuilder().setEventName("test").emit();

    assertThat(exporter.getFinishedLogRecordItems())
        .satisfiesExactly(
            // Does not have new entity.
            logRecord ->
                assertThat(EntityUtil.getEntities(logRecord.getResource())).doesNotContain(entity),
            // Has new entity.
            logRecord ->
                assertThat(EntityUtil.getEntities(logRecord.getResource())).contains(entity));
  }
}
