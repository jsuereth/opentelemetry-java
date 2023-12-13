/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.exemplar;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.testing.time.TestClock;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MinMaxVarExemplarReservoirTest {

  @Test
  public void twoMeasurementsAreMinMax() {
    TestClock clock = TestClock.create();
    Random mockRandom =
        new Random() {
          @Override
          public int nextInt(int max) {
            switch (max) {
                // Force one sample in bucket 1 and two in bucket 0.
              case 2:
                return 1;
              default:
                return 0;
            }
          }
        };
    ExemplarReservoir<DoubleExemplarData> reservoir =
        ExemplarReservoir.doubleMinMaxVarOptReservoir(clock, 4, () -> mockRandom);

    // Max next
    reservoir.offerDoubleMeasurement(1, Attributes.empty(), Context.root());
    // Min first
    reservoir.offerDoubleMeasurement(-1, Attributes.empty(), Context.root());

    // Ensure we see both
    assertThat(reservoir.collectAndReset(Attributes.empty()))
        .satisfiesExactlyInAnyOrder(
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(1);
            },
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(-1);
            });
  }

  @Test
  public void manyMeasurementsIncreasingMax() {
    TestClock clock = TestClock.create();
    Random mockRandom =
        new Random() {
          @Override
          public int nextInt(int max) {
            switch (max) {
                // Force one sample in bucket 1 and two in bucket 0.
              default:
                return max % 2;
            }
          }
        };
    ExemplarReservoir<DoubleExemplarData> reservoir =
        ExemplarReservoir.doubleMinMaxVarOptReservoir(clock, 4, () -> mockRandom);

    // We contineu to increase the maximum value.
    // In this event, the algorithm should preserve HALF SIZE exemplars
    // in the "outside variance" bucket, and then the min and max.
    for (int i = 0; i < 10; i++) {
      reservoir.offerDoubleMeasurement(i, Attributes.empty(), Context.root());
    }

    // Ensure we see both
    assertThat(reservoir.collectAndReset(Attributes.empty()))
        .satisfiesExactlyInAnyOrder(
            // Min
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(0);
            },
            // Max
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(9);
            },
            // Last High Variance seen values (guaranteed by random override)
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(8);
            },
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(7);
            });
  }

  @Test
  public void manyMeasurementsFillBothReservoirs() {
    TestClock clock = TestClock.create();
    Random mockRandom =
        new Random() {
          @Override
          public int nextInt(int max) {
            switch (max) {
                // Force one sample in bucket 1 and two in bucket 0.
              default:
                return max % 2;
            }
          }
        };
    ExemplarReservoir<DoubleExemplarData> reservoir =
        ExemplarReservoir.doubleMinMaxVarOptReservoir(clock, 4, () -> mockRandom);

    // We contineu to increase the maximum value.
    // In this event, the algorithm should preserve HALF SIZE exemplars
    // in the "outside variance" bucket, and then the min and max.
    for (int i = -10; i < 10; i++) {
      reservoir.offerDoubleMeasurement(i, Attributes.empty(), Context.root());
    }
    // Now offer a bunch of values inside the variance
    for (int i = 0; i < 20; i++) {
      reservoir.offerDoubleMeasurement(i / 4.0, Attributes.empty(), Context.root());
    }

    // Ensure we see both
    assertThat(reservoir.collectAndReset(Attributes.empty()))
        .satisfiesExactlyInAnyOrder(
            // Min
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(-10);
            },
            // Max
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(9);
            },
            // Last High Variance seen values (guaranteed by random override)
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(8);
            },
            exemplar -> {
              assertThat(exemplar.getEpochNanos()).isEqualTo(clock.now());
              assertThat(exemplar.getValue()).isEqualTo(7);
            },
            // Last Inside variance seen values (guaranteed by random override)
            exemplar -> {
              assertThat(exemplar.getValue()).isEqualTo(4.5);
            },
            exemplar -> {
              assertThat(exemplar.getValue()).isEqualTo(4.75);
            });
  }
}
