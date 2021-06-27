/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.testing.assertj.metrics;

import io.opentelemetry.sdk.metrics.data.Exemplar;
import io.opentelemetry.sdk.metrics.data.SampledPointData;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.Assertions;

public class AbstractSampledPointDataAssert<
        PointAssertT extends AbstractSampledPointDataAssert<PointAssertT, PointT>,
        PointT extends SampledPointData>
    extends AbstractPointDataAssert<PointAssertT, PointT> {
  protected AbstractSampledPointDataAssert(PointT actual, Class<PointAssertT> assertClass) {
    super(actual, assertClass);
  }

  public AbstractIterableAssert<?, ? extends Iterable<? extends Exemplar>, Exemplar, ?>
      exemplars() {
    isNotNull();
    return Assertions.assertThat(actual.getExemplars());
  }

  public PointAssertT hasExemplars(Exemplar... exemplars) {
    isNotNull();
    Assertions.assertThat(actual.getExemplars())
        .as("exemplars")
        .containsExactlyInAnyOrder(exemplars);
    return myself;
  }
}
