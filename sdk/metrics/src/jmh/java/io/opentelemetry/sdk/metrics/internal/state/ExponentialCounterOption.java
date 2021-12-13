/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import java.util.function.Supplier;

/** Options of counters for exponential counter benchmark. */
@SuppressWarnings("ImmutableEnumChecker")
public enum ExponentialCounterOption {
  MAP_COUNTER(() -> new MapCounter()),
  CIRCULAR_FIXED_BUFFER_COUNTER(() -> new CircularBufferCounter());

  private final Supplier<ExponentialCounter> counterSupplier;

  private ExponentialCounterOption(Supplier<ExponentialCounter> counterSupplier) {
    this.counterSupplier = counterSupplier;
  }

  public ExponentialCounter getCounter() {
    return this.counterSupplier.get();
  }
}
