package com.smoketurner.snowizard.benchmarks;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import com.smoketurner.snowizard.core.IdWorker;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Group)
public class IdWorkerBenchmark {

    private final IdWorker worker = IdWorker.builder(1, 1).build();

    @Benchmark
    @Group("no_contention")
    @GroupThreads(1)
    public void no_contention_nextId() throws Exception {
        worker.nextId();
    }

    @Benchmark
    @Group("mild_contention")
    @GroupThreads(2)
    public void mild_contention_nextId() throws Exception {
        worker.nextId();
    }

    @Benchmark
    @Group("high_contention")
    @GroupThreads(8)
    public void high_contention_nextId() throws Exception {
        worker.nextId();
    }

    public static void main(String[] args) throws Exception {
        final Options opt = new OptionsBuilder()
                .include(IdWorkerBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
