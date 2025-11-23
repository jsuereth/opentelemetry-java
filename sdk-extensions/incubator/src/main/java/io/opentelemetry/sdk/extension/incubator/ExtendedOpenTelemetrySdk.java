/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.extension.incubator;

import io.opentelemetry.api.incubator.ExtendedOpenTelemetry;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.incubator.entity.Entity;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.entity.internal.SdkEntity;
import io.opentelemetry.sdk.extension.incubator.entity.internal.ExtendedEntityUtil;
import io.opentelemetry.sdk.extension.incubator.fileconfig.SdkConfigProvider;
import io.opentelemetry.sdk.logs.internal.SdkLoggerProviderUtil;
import io.opentelemetry.sdk.trace.internal.SdkTracerProviderUtil;
import java.io.Closeable;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/** A new interface for creating OpenTelemetrySdk that supports getting {@link ConfigProvider}. */
public final class ExtendedOpenTelemetrySdk extends OpenTelemetrySdk
    implements ExtendedOpenTelemetry, Closeable {

  private final OpenTelemetrySdk openTelemetrySdk;
  private final ObfuscatedConfigProvider configProvider;

  private ExtendedOpenTelemetrySdk(
      OpenTelemetrySdk openTelemetrySdk, SdkConfigProvider configProvider) {
    super(
        openTelemetrySdk.getSdkTracerProvider(),
        openTelemetrySdk.getSdkMeterProvider(),
        openTelemetrySdk.getSdkLoggerProvider(),
        openTelemetrySdk.getPropagators());
    this.openTelemetrySdk = openTelemetrySdk;
    this.configProvider = new ObfuscatedConfigProvider(configProvider);
  }

  public static ExtendedOpenTelemetrySdk create(
      OpenTelemetrySdk openTelemetrySdk, SdkConfigProvider sdkConfigProvider) {
    return new ExtendedOpenTelemetrySdk(openTelemetrySdk, sdkConfigProvider);
  }

  @Override
  public ConfigProvider getConfigProvider() {
    return configProvider;
  }

  /** Returns the {@link SdkConfigProvider} for this {@link ExtendedOpenTelemetrySdk}. */
  public SdkConfigProvider getSdkConfigProvider() {
    return configProvider.unobfuscate();
  }

  @Override
  public String toString() {
    return "ExtendedOpenTelemetrySdk{"
        + "openTelemetrySdk="
        + openTelemetrySdk
        + ", configProvider="
        + configProvider.unobfuscate()
        + "}";
  }

  /**
   * This class allows the SDK to unobfuscate an obfuscated provider.
   *
   * <p>Static global providers are obfuscated when they are returned from the API to prevent users
   * from casting them to their SDK specific implementation. For example, we do not want users to
   * use patterns like {@code (SdkConfigProvider) openTelemetry.getConfigProvider()}.
   */
  @ThreadSafe
  private static class ObfuscatedConfigProvider implements ConfigProvider {

    private final SdkConfigProvider delegate;

    private ObfuscatedConfigProvider(SdkConfigProvider delegate) {
      this.delegate = delegate;
    }

    @Override
    @Nullable
    public DeclarativeConfigProperties getInstrumentationConfig() {
      return delegate.getInstrumentationConfig();
    }

    private SdkConfigProvider unobfuscate() {
      return delegate;
    }
  }

  @Override
  public ExtendedOpenTelemetry withEntity(Entity e) {
    // TODO - this could throw a class-cast exception, do we need to handle this?
    SdkEntity sdkEntity = ExtendedEntityUtil.convertEntity(e);
    return ExtendedOpenTelemetrySdk.create(
        OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProviderUtil.withEntity(getSdkTracerProvider(), sdkEntity))
            .setLoggerProvider(SdkLoggerProviderUtil.withEntity(getSdkLoggerProvider(), sdkEntity))
            // TODO - withEntity
            .setMeterProvider(getSdkMeterProvider())
            .setPropagators(getPropagators())
            .build(),
        getSdkConfigProvider());
  }
}
