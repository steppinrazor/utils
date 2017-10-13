package com.krs.jmh;

import com.krs.utils.TimeStampedGUID;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by KR Shabazz on 3/24/16 1:07 AM.
 * <p>
 * JMH Samples referenced:
 * <p>
 * - http://hg.openjdk.java.net/code-tools/jmh/file/f4e8d0d61f1f/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_02_BenchmarkModes.java
 * - http://hg.openjdk.java.net/code-tools/jmh/file/f4e8d0d61f1f/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_27_Params.java
 */
@State(Scope.Benchmark)
public class BenchmarkTimeStampedGUID {
    static int limit = 1000;

    static {
        System.setProperty("guidYearOffset", "2000");
    }

    /*Supplier<UUID> supplier;
    Iterator<UUID> iterator;
    Stream<UUID> stream;

    @Setup
    public void beforeMeasure(){
        supplier = TimeStampedGUID.supplier();
        iterator = TimeStampedGUID.iterator();
        stream = TimeStampedGUID.stream();
    }*/

    public static void main(String[] args) throws RunnerException {
        Options o = new OptionsBuilder()
                .include(BenchmarkTimeStampedGUID.class.getSimpleName())
                .warmupIterations(1)
                .measurementIterations(10)
                .resultFormat(ResultFormatType.JSON)
                .operationsPerInvocation(limit)
                .forks(1)
                .build();

        new Runner(o).run();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SingleShotTime, Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void measureIterator() {
        Iterator<UUID> iterator = TimeStampedGUID.iterator();
        int i = limit;
        while (i-- > 0)
            iterator.next();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SingleShotTime, Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void measureStream() {
        Stream<UUID> stream = TimeStampedGUID.stream();
        stream.limit(limit).forEach((UUID) -> {/*DO NOTHING*/});
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SingleShotTime, Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void measureSupplier() {
        Supplier<UUID> supplier = TimeStampedGUID.supplier();
        int i = limit;
        while (i-- > 0)
            supplier.get();
    }
}
