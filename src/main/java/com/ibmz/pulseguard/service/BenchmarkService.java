package com.ibmz.pulseguard.service;

import com.ibmz.pulseguard.engine.SepsisRiskEngine;
import com.ibmz.pulseguard.model.PatientVital;

import java.util.Arrays;

/**
 * BenchmarkService: Benchmarks the real-time inference latency and throughput
 * of the PulseGuard-Z engine.
 */
public class BenchmarkService {

    public static class BenchmarkResult {
        public int iterations;
        public double totalTimeMs;
        public double avgLatencyMicros;
        public double p50LatencyMicros;
        public double p95LatencyMicros;
        public double p99LatencyMicros;
        public double minLatencyMicros;
        public double maxLatencyMicros;
        public double throughputInferencesPerSec;
        public String osArch;
        public String osName;
        public int availableCores;
        public double heapUsedMb;
        public String ibmZOptimizationStatus;
    }

    public static BenchmarkResult runBenchmark(int iterations) {
        if (iterations <= 0) iterations = 50_000;
        if (iterations > 500_000) iterations = 500_000;

        // Synthetic telemetry packet
        PatientVital testVital = new PatientVital(
            "TEST-P99", "BED-BENCH", System.currentTimeMillis(),
            115.0, 85.0, 52.0, 26.0, 38.9, 89.0, 4.3, 18.2
        );

        // Warm up JIT compiler
        for (int i = 0; i < 5_000; i++) {
            SepsisRiskEngine.evaluate(testVital);
        }

        double[] latencies = new double[iterations];
        long startTotalNano = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long s = System.nanoTime();
            SepsisRiskEngine.evaluate(testVital);
            long e = System.nanoTime();
            latencies[i] = (e - s) / 1000.0; // microseconds
        }

        long endTotalNano = System.nanoTime();
        double totalTimeMs = (endTotalNano - startTotalNano) / 1_000_000.0;

        Arrays.sort(latencies);

        double sum = 0;
        for (double d : latencies) sum += d;
        double avg = sum / iterations;

        BenchmarkResult res = new BenchmarkResult();
        res.iterations = iterations;
        res.totalTimeMs = totalTimeMs;
        res.avgLatencyMicros = avg;
        res.minLatencyMicros = latencies[0];
        res.p50LatencyMicros = latencies[(int) (iterations * 0.50)];
        res.p95LatencyMicros = latencies[(int) (iterations * 0.95)];
        res.p99LatencyMicros = latencies[(int) (iterations * 0.99)];
        res.maxLatencyMicros = latencies[iterations - 1];
        res.throughputInferencesPerSec = (iterations / totalTimeMs) * 1000.0;

        res.osArch = System.getProperty("os.arch");
        res.osName = System.getProperty("os.name");
        res.availableCores = Runtime.getRuntime().availableProcessors();
        long usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        res.heapUsedMb = usedBytes / (1024.0 * 1024.0);

        if (res.osArch.equalsIgnoreCase("s390x")) {
            res.ibmZOptimizationStatus = "NATIVE_S390X: Running on IBM Z / LinuxONE enterprise mainframe with hardware vector acceleration.";
        } else {
            res.ibmZOptimizationStatus = "EMULATED_X86/ARM: Running in workstation dev mode. Deployable directly to IBM LinuxONE Community Cloud (s390x).";
        }

        return res;
    }
}
