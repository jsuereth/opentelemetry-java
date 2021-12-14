/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Measurement(iterations = 10, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(1)
@Threads(value = 1)
public class ExponentialCounterBenchmarks {

  @State(Scope.Thread)
  public static class ThreadState {
    @Param({"100", "1000"})
    int recordings;

    @Param ExponentialCounterIndexGen yIndexOption;
    @Param ExponentialCounterOption zCounterOption;

    private ExponentialCounter counter;

    @Setup(Level.Iteration)
    public final void setup() {
      this.counter = zCounterOption.getCounter();
    }

    public void record() {
      for (int i = 0; i < this.recordings; i++) {
        int idx = this.yIndexOption.getIndex();
        this.counter.increment(idx, 1);
      }
    }
  }

  @Benchmark
  public void increment(ThreadState threadState) {
    threadState.record();
  }
}
