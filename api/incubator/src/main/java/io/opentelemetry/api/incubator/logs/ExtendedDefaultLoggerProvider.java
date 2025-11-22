/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.incubator.logs;

import io.opentelemetry.api.incubator.entity.Entity;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;
import io.opentelemetry.api.logs.LoggerProvider;

public class ExtendedDefaultLoggerProvider implements ExtendedLoggerProvider {

  private static final LoggerProvider INSTANCE = new ExtendedDefaultLoggerProvider();
  private static final LoggerBuilder NOOP_BUILDER = new NoopLoggerBuilder();

  private ExtendedDefaultLoggerProvider() {}

  public static LoggerProvider getNoop() {
    return INSTANCE;
  }

  @Override
  public LoggerBuilder loggerBuilder(String instrumentationScopeName) {
    return NOOP_BUILDER;
  }

  private static class NoopLoggerBuilder implements LoggerBuilder {

    @Override
    public LoggerBuilder setSchemaUrl(String schemaUrl) {
      return this;
    }

    @Override
    public LoggerBuilder setInstrumentationVersion(String instrumentationVersion) {
      return this;
    }

    @Override
    public Logger build() {
      return ExtendedDefaultLogger.getNoop();
    }
  }

  @Override
  public ExtendedLoggerProvider withEntity(Entity entity) {
    // Default is noop.
    return this;
  }
}
