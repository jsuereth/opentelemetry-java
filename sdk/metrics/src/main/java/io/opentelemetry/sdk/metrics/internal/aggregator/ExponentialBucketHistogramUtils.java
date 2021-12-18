/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.aggregator;

/**
 * Utilities for interacting with exponential bucket histograms.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class ExponentialBucketHistogramUtils {
  private ExponentialBucketHistogramUtils() {}

  /** Maximum scale used in OpenTelemetry. */
  public static final int MAX_SCALE = 20;

  /** Maximum number of buckets (for positive or negative). */
  public static final int MAX_BUCKETS = 320;

  // TODO - We could hardcode these values rather than spin them up at startup, possibly
  // use codegen.
  private static final double[] SCALE_FACTOR_CACHE = new double[MAX_SCALE + 1];

  static {
    for (int i = 0; i <= MAX_SCALE; i++) {
      SCALE_FACTOR_CACHE[i] = Math.scalb(1D / Math.log(2), i);
    }
  }

  private static double scaleFactor(int scale) {
    if (scale < 0 || scale > MAX_SCALE) {
      return Math.scalb(1D / Math.log(2), scale);
    }
    return SCALE_FACTOR_CACHE[scale];
  }

  /** Implementation of value->index that uses simple logarithms. */
  private static int naiveValueToIndex(int scale, double value) {
    return (int) Math.floor(Math.log(value) * scaleFactor(scale));
  }

  /** Convert a double value into an exponential bucket index based on the "scale" of buckets. */
  public static int valueToIndex(int scale, double value) {
    if (false) {
      return naiveValueToIndex(scale, value);
    }
    return exponentValueToIndex(scale, value);
  }

  public static int exponentValueToIndex(int scale, double value) {
    return getBase2(value) >> -scale;
  }

  // MinValue is the smallest normal number.
  private static final double MinValue = 0x1p-1022;

  // SignificandWidth is the size of an IEEE 754 double-precision
  // floating-point significand.
  private static final int SignificandWidth = 52;
  // ExponentWidth is the size of an IEEE 754 double-precision
  // floating-point exponent.
  private static final int ExponentWidth = 11;

  // SignificandMask is the mask for the significand of an IEEE 754
  // double-precision floating-point value: 0xFFFFFFFFFFFFF.
  // private static final int SignificandMask = 1 << SignificandWidth - 1;

  // ExponentBias is the exponent bias specified for encoding
  // the IEEE 754 double-precision floating point exponent: 1023
  private static final int ExponentBias = (1 << (ExponentWidth - 1)) - 1;

  // ExponentMask are set to 1 for the bits of an IEEE 754
  // floating point exponent: 0x7FF0000000000000
  private static final long ExponentMask = ((1 << ExponentWidth) - 1) << SignificandWidth;

  // SignMask selects the sign bit of an IEEE 754 floating point
  // number.
  // private static final long SignMask = (1 << 63);

  // MinNormalExponent is the minimum exponent of a normalized
  // floating point: -1022
  private static final int MinNormalExponent = -ExponentBias + 1;

  // getBase2 extracts the normalized base-2 fractional exponent.  Like
  // math.Frexp(), rounds subnormal values up to the minimum normal
  // value.  Unlike Frexp(), this returns k for the equation f x 2**k
  // where f is in the range [1, 2).
  private static int getBase2(double value) {
    if (value <= MinValue) {
      return MinNormalExponent;
    }
    long rawBits = Double.doubleToRawLongBits(value);
    long rawExponent = (rawBits & ExponentMask) >> SignificandWidth;
    return (int) (rawExponent - ExponentBias);
  }
}
