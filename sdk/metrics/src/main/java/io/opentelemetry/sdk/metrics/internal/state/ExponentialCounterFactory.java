/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

/** A mechanism of constructing Exponential Counters that allows creation + copying. */
public interface ExponentialCounterFactory {
  /** Create a new exponential counter. These allow flexible bucket strategies. */
  ExponentialCounter create();
  /** Copy buckets from another counter into this one. */
  ExponentialCounter copy(ExponentialCounter other);
}
