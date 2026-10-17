package com.ibmz.pulseguard.server;

import com.ibmz.pulseguard.engine.SepsisRiskEngine;
import com.ibmz.pulseguard.model.PatientBed;
import com.ibmz.pulseguard.model.PatientVital;
import com.ibmz.pulseguard.model.TriageAlert;
import com.ibmz.pulseguard.service.BenchmarkService;
import com.ibmz.pulseguard.service.TelemetrySimulator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * PulseGuardServer: Lightweight embedded HTTP server delivering real-time REST APIs
 * and the clinical triage web UI.
 */
public class PulseGuardServer {

    private final int port;
    private final TelemetrySimulator simulator;
    private HttpServer server;

    public PulseGuardServer(int port, TelemetrySimulator simulator) {
        this.port = port;
        this.simulator = simulator;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/api/v1/beds", new BedsHandler());
        server.createContext("/api/v1/telemetry", new TelemetryHandler());
        server.createContext("/api/v1/benchmark", new BenchmarkHandler());
        server.createContext("/api/v1/system", new SystemHandler());
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println(">>> PulseGuard-Z Server successfully started on port " + port);
        System.out.println(">>> Clinical Dashboard: http://localhost:" + port + "/");
        System.out.println(">>> Real-Time API:     http://localhost:" + port + "/api/v1/beds");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- HTTP Handlers ---

    private class BedsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"status\":\"success\",\"timestamp\":").append(System.currentTimeMillis()).append(",\"beds\":[");

            List<PatientBed> beds = simulator.getBeds();
            for (int i = 0; i < beds.size(); i++) {
                PatientBed b = beds.get(i);
                PatientVital v = b.getLatestVital();
                TriageAlert a = b.getCurrentAlert();

                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"bedId\":\"").append(b.getBedId()).append("\",");
                sb.append("\"patientId\":\"").append(b.getPatientId()).append("\",");
                sb.append("\"patientName\":\"").append(b.getPatientName()).append("\",");
                sb.append("\"age\":").append(b.getAge()).append(",");
                sb.append("\"diagnosis\":\"").append(b.getPrimaryDiagnosis()).append("\",");

                if (v != null) {
                    sb.append("\"vital\":{");
                    sb.append("\"heartRate\":").append(String.format("%.1f", v.getHeartRate())).append(",");
                    sb.append("\"systolicBp\":").append(String.format("%.1f", v.getSystolicBp())).append(",");
                    sb.append("\"diastolicBp\":").append(String.format("%.1f", v.getDiastolicBp())).append(",");
                    sb.append("\"respRate\":").append(String.format("%.1f", v.getRespRate())).append(",");
                    sb.append("\"temperatureC\":").append(String.format("%.1f", v.getTemperatureC())).append(",");
                    sb.append("\"spO2\":").append(String.format("%.1f", v.getSpO2())).append(",");
                    sb.append("\"lactate\":").append(String.format("%.2f", v.getLactate())).append(",");
                    sb.append("\"whiteBloodCell\":").append(String.format("%.1f", v.getWhiteBloodCell())).append(",");
                    sb.append("\"map\":").append(String.format("%.1f", v.getMeanArterialPressure())).append(",");
                    sb.append("\"shockIndex\":").append(String.format("%.2f", v.getShockIndex()));
                    sb.append("},");
                }

                if (a != null) {
                    sb.append("\"alert\":{");
                    sb.append("\"level\":\"").append(a.getLevel().name()).append("\",");
                    sb.append("\"riskScore\":").append(String.format("%.3f", a.getSepsisRiskScore())).append(",");
                    sb.append("\"mortalityRisk\":").append(String.format("%.3f", a.getFourHourMortalityRisk())).append(",");
                    sb.append("\"latencyMicros\":").append(String.format("%.2f", a.getInferenceLatencyMicros())).append(",");
                    sb.append("\"recommendation\":\"").append(escapeJson(a.getClinicalRecommendation())).append("\",");
                    sb.append("\"biomarkers\":[");
                    List<String> bio = a.getContributingBiomarkers();
                    for (int j = 0; j < bio.size(); j++) {
                        if (j > 0) sb.append(",");
                        sb.append("\"").append(escapeJson(bio.get(j))).append("\"");
                    }
                    sb.append("]");
                    sb.append("}");
                }

                sb.append("}");
            }

            sb.append("]}");
            sendResponse(exchange, 200, "application/json", sb.toString());
        }
    }

    private static class TelemetryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed. Use POST.\"}");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            // Simple robust key extraction from JSON
            String pId = extractJsonString(body, "patientId", "P-UNKNOWN");
            String bId = extractJsonString(body, "bedId", "BED-CUSTOM");
            double hr = extractJsonDouble(body, "heartRate", 80.0);
            double sbp = extractJsonDouble(body, "systolicBp", 115.0);
            double dbp = extractJsonDouble(body, "diastolicBp", 75.0);
            double rr = extractJsonDouble(body, "respRate", 16.0);
            double temp = extractJsonDouble(body, "temperatureC", 37.0);
            double spo2 = extractJsonDouble(body, "spO2", 98.0);
            double lactate = extractJsonDouble(body, "lactate", 1.2);
            double wbc = extractJsonDouble(body, "whiteBloodCell", 7.5);

            PatientVital vital = new PatientVital(pId, bId, System.currentTimeMillis(), hr, sbp, dbp, rr, temp, spo2, lactate, wbc);
            TriageAlert alert = SepsisRiskEngine.evaluate(vital);

            StringBuilder res = new StringBuilder();
            res.append("{");
            res.append("\"status\":\"evaluated\",");
            res.append("\"patientId\":\"").append(alert.getPatientId()).append("\",");
            res.append("\"bedId\":\"").append(alert.getBedId()).append("\",");
            res.append("\"triageLevel\":\"").append(alert.getLevel().name()).append("\",");
            res.append("\"sepsisRiskScore\":").append(String.format("%.4f", alert.getSepsisRiskScore())).append(",");
            res.append("\"fourHourMortalityRisk\":").append(String.format("%.4f", alert.getFourHourMortalityRisk())).append(",");
            res.append("\"inferenceLatencyMicros\":").append(String.format("%.2f", alert.getInferenceLatencyMicros())).append(",");
            res.append("\"meanArterialPressure\":").append(String.format("%.1f", vital.getMeanArterialPressure())).append(",");
            res.append("\"shockIndex\":").append(String.format("%.2f", vital.getShockIndex())).append(",");
            res.append("\"recommendation\":\"").append(escapeJson(alert.getClinicalRecommendation())).append("\",");
            res.append("\"biomarkers\":[");
            List<String> bio = alert.getContributingBiomarkers();
            for (int i = 0; i < bio.size(); i++) {
                if (i > 0) res.append(",");
                res.append("\"").append(escapeJson(bio.get(i))).append("\"");
            }
            res.append("]");
            res.append("}");

            sendResponse(exchange, 200, "application/json", res.toString());
        }
    }

    private static class BenchmarkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            BenchmarkService.BenchmarkResult b = BenchmarkService.runBenchmark(50_000);

            StringBuilder res = new StringBuilder();
            res.append("{");
            res.append("\"iterations\":").append(b.iterations).append(",");
            res.append("\"totalTimeMs\":").append(String.format("%.2f", b.totalTimeMs)).append(",");
            res.append("\"avgLatencyMicros\":").append(String.format("%.3f", b.avgLatencyMicros)).append(",");
            res.append("\"p50LatencyMicros\":").append(String.format("%.3f", b.p50LatencyMicros)).append(",");
            res.append("\"p95LatencyMicros\":").append(String.format("%.3f", b.p95LatencyMicros)).append(",");
            res.append("\"p99LatencyMicros\":").append(String.format("%.3f", b.p99LatencyMicros)).append(",");
            res.append("\"throughputInferencesPerSec\":").append(String.format("%.0f", b.throughputInferencesPerSec)).append(",");
            res.append("\"osArch\":\"").append(b.osArch).append("\",");
            res.append("\"osName\":\"").append(b.osName).append("\",");
            res.append("\"availableCores\":").append(b.availableCores).append(",");
            res.append("\"heapUsedMb\":").append(String.format("%.1f", b.heapUsedMb)).append(",");
            res.append("\"ibmZOptimizationStatus\":\"").append(escapeJson(b.ibmZOptimizationStatus)).append("\"");
            res.append("}");

            sendResponse(exchange, 200, "application/json", res.toString());
        }
    }

    private static class SystemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            String arch = System.getProperty("os.arch");
            String res = "{" +
                "\"service\":\"PulseGuard-Z Enterprise Triage\"," +
                "\"version\":\"1.0.0-IBM-Z\"," +
                "\"architecture\":\"" + arch + "\"," +
                "\"isLinuxONE\":" + ("s390x".equalsIgnoreCase(arch)) + "," +
                "\"jvm\":\"" + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\"," +
                "\"os\":\"" + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\"," +
                "\"cores\":" + Runtime.getRuntime().availableProcessors() + "," +
                "\"maxMemoryMb\":" + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) +
                "}";

            sendResponse(exchange, 200, "application/json", res);
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            InputStream is = getClass().getResourceAsStream("/static" + path);
            if (is == null) {
                // Fallback to local filesystem if not in classpath
                File f = new File("src/main/resources/static" + path);
                if (f.exists()) {
                    is = new FileInputStream(f);
                }
            }

            if (is == null) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found");
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = is.readAllBytes();
            is.close();

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private static void sendCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    private static void sendResponse(HttpExchange exchange, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static String extractJsonString(String json, String key, String defaultVal) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx == -1) return defaultVal;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return defaultVal;
        int startQuote = json.indexOf("\"", colon);
        if (startQuote == -1) return defaultVal;
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return defaultVal;
        return json.substring(startQuote + 1, endQuote);
    }

    private static double extractJsonDouble(String json, String key, double defaultVal) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx == -1) return defaultVal;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return defaultVal;
        int start = colon + 1;
        while (start < json.length() && (Character.isWhitespace(json.charAt(start)) || json.charAt(start) == '\"')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        if (start < end) {
            try {
                return Double.parseDouble(json.substring(start, end));
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }
}
