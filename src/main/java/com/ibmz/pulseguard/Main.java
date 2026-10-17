package com.ibmz.pulseguard;

import com.ibmz.pulseguard.audit.CryptographicAuditLedger;
import com.ibmz.pulseguard.server.PulseGuardServer;
import com.ibmz.pulseguard.service.BenchmarkService;
import com.ibmz.pulseguard.service.TelemetrySimulator;
import com.ibmz.pulseguard.teleicu.DistrictTriageRouter;

public class Main {

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        printBanner(port);

        // Run warmup benchmark
        System.out.println("[INIT] Running warm-up real-time inference benchmark (20,000 iterations)...");
        BenchmarkService.BenchmarkResult bench = BenchmarkService.runBenchmark(20_000);
        System.out.printf("[BENCHMARK] Avg Latency: %.3f µs | P99 Latency: %.3f µs | Throughput: %,.0f decisions/sec\n",
                bench.avgLatencyMicros, bench.p99LatencyMicros, bench.throughputInferencesPerSec);
        System.out.printf("[PLATFORM]  Architecture: %s | OS: %s | JVM: %s\n",
                bench.osArch, bench.osName, System.getProperty("java.version"));
        System.out.println("[SECURITY]  Pervasive Encryption mode active for patient vitals in-flight & at-rest.");
        System.out.println("--------------------------------------------------------------------------------");

        CryptographicAuditLedger auditLedger = new CryptographicAuditLedger();
        DistrictTriageRouter triageRouter = new DistrictTriageRouter();
        TelemetrySimulator simulator = new TelemetrySimulator(auditLedger, triageRouter);

        PulseGuardServer server = null;
        int[] tryPorts = { port, 8085, 9090, 8000 };
        for (int p : tryPorts) {
            try {
                server = new PulseGuardServer(p, simulator, auditLedger, triageRouter);
                server.start();
                port = p;
                break;
            } catch (java.net.BindException be) {
                System.out.printf("[WARN] Port %d in use, trying next port...\n", p);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to start PulseGuard-Z Server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        if (server == null) {
            System.err.println("[FATAL] Could not bind to any candidate port.");
            System.exit(1);
        }

        final PulseGuardServer finalServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Terminating PulseGuard-Z telemetry engine...");
            simulator.shutdown();
            finalServer.stop();
            System.out.println("[SHUTDOWN] Clean exit.");
        }));
    }

    private static void printBanner(int port) {
        System.out.println("""
            ================================================================================
               ___        _           ___                     _       ____ 
              / _ \\ _   _| |___  ___ / _ \\ _   _  __ _ _ __ __| |     |__  /
             / /_)/| | | | / __|/ _ \\ | | | | | |/ _` | '__/ _` |_____  / / 
            / ___/ | |_| | \\__ \\  __/ |_| | |_| | (_| | | | (_| |_____|/ /_ 
            \\/      \\__,_|_|___/\\___|\\__\\_\\\\__,_|\\__,_|_|  \\__,_|     /____|
            
              Real-Time AI for Critical Decisions | IBM Z Datathon 2026
              Team: CodeBuilders | Architecture: s390x / LinuxONE Ready
            ================================================================================
            """);
    }
}
