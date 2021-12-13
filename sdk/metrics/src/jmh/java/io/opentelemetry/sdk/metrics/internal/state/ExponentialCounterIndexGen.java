/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

@SuppressWarnings("ImmutableEnumChecker")
public enum ExponentialCounterIndexGen {
  FULL_RANDOM(() -> ThreadLocalRandom.current().nextInt(320)),
  RANDOM_WITHIN_20(() -> ThreadLocalRandom.current().nextInt(20)),
  SINGLE_VALUE(() -> 1);

  private final IntSupplier indexSupplier;

  private ExponentialCounterIndexGen(IntSupplier indexSupplier) {
    this.indexSupplier = indexSupplier;
  }

  public int getIndex() {
    return this.indexSupplier.getAsInt();
  }
}
