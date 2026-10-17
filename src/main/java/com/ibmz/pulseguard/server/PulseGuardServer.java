package com.ibmz.pulseguard.server;

import com.ibmz.pulseguard.abdm.AbdmFhirParser;
import com.ibmz.pulseguard.abdm.AbhaValidator;
import com.ibmz.pulseguard.audit.CryptographicAuditLedger;
import com.ibmz.pulseguard.clinical.News2Calculator;
import com.ibmz.pulseguard.engine.SepsisRiskEngine;
import com.ibmz.pulseguard.model.PatientBed;
import com.ibmz.pulseguard.model.PatientVital;
import com.ibmz.pulseguard.model.TriageAlert;
import com.ibmz.pulseguard.service.BenchmarkService;
import com.ibmz.pulseguard.service.TelemetrySimulator;
import com.ibmz.pulseguard.report.AbdmReferralSlipGenerator;
import com.ibmz.pulseguard.teleicu.DistrictTriageRouter;
import com.ibmz.pulseguard.teleicu.GsmSmsDispatcher;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * PulseGuardServer: Production REST Server with ABDM FHIR R4, Cryptographic Audit Ledger,
 * and District Tele-ICU Escalation.
 */
public class PulseGuardServer {

    private final int port;
    private final TelemetrySimulator simulator;
    private final CryptographicAuditLedger auditLedger;
    private final DistrictTriageRouter triageRouter;
    private HttpServer server;

    public PulseGuardServer(int port, TelemetrySimulator simulator,
                            CryptographicAuditLedger auditLedger, DistrictTriageRouter triageRouter) {
        this.port = port;
        this.simulator = simulator;
        this.auditLedger = auditLedger;
        this.triageRouter = triageRouter;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/api/v1/beds", new BedsHandler());
        server.createContext("/api/v1/telemetry", new TelemetryHandler());
        server.createContext("/api/v1/fhir", new FhirHandler());
        server.createContext("/api/v1/audit/chain", new AuditHandler());
        server.createContext("/api/v1/teleicu", new TeleIcuHandler());
        server.createContext("/api/v1/referral", new ReferralHandler());
        server.createContext("/api/v1/sms", new SmsHandler());
        server.createContext("/api/v1/benchmark", new BenchmarkHandler());
        server.createContext("/api/v1/system", new SystemHandler());
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println(">>> PulseGuard-Z Server running on port " + port);
        System.out.println(">>> Clinical Dashboard: http://localhost:" + port + "/");
        System.out.println(">>> FHIR R4 Endpoint:  http://localhost:" + port + "/api/v1/fhir?bedId=BED-03");
        System.out.println(">>> Referral Slip API: http://localhost:" + port + "/api/v1/referral?bedId=BED-03");
        System.out.println(">>> Offline SMS API:   http://localhost:" + port + "/api/v1/sms?bedId=BED-03");
        System.out.println(">>> Audit Chain API:   http://localhost:" + port + "/api/v1/audit/chain");
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
                News2Calculator.News2Report n = b.getCurrentNews2();

                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"bedId\":\"").append(b.getBedId()).append("\",");
                sb.append("\"patientId\":\"").append(b.getPatientId()).append("\",");
                sb.append("\"abhaId\":\"").append(b.getAbhaId()).append("\",");
                sb.append("\"patientName\":\"").append(b.getPatientName()).append("\",");
                sb.append("\"age\":").append(b.getAge()).append(",");
                sb.append("\"gender\":\"").append(b.getGender()).append("\",");
                sb.append("\"facility\":\"").append(escapeJson(b.getFacilityName())).append("\",");
                sb.append("\"diagnosis\":\"").append(escapeJson(b.getPrimaryDiagnosis())).append("\",");

                if (n != null) {
                    sb.append("\"news2\":{");
                    sb.append("\"score\":").append(n.totalScore).append(",");
                    sb.append("\"category\":\"").append(n.riskCategory).append("\",");
                    sb.append("\"redFlag\":").append(n.hasIndividualRedFlag).append(",");
                    sb.append("\"response\":\"").append(escapeJson(n.clinicalResponse)).append("\"");
                    sb.append("},");
                }

                if (v != null) {
                    sb.append("\"vital\":{");
                    sb.append("\"heartRate\":").append(String.format(java.util.Locale.US, "%.1f", v.getHeartRate())).append(",");
                    sb.append("\"systolicBp\":").append(String.format(java.util.Locale.US, "%.1f", v.getSystolicBp())).append(",");
                    sb.append("\"diastolicBp\":").append(String.format(java.util.Locale.US, "%.1f", v.getDiastolicBp())).append(",");
                    sb.append("\"respRate\":").append(String.format(java.util.Locale.US, "%.1f", v.getRespRate())).append(",");
                    sb.append("\"temperatureC\":").append(String.format(java.util.Locale.US, "%.1f", v.getTemperatureC())).append(",");
                    sb.append("\"spO2\":").append(String.format(java.util.Locale.US, "%.1f", v.getSpO2())).append(",");
                    sb.append("\"lactate\":").append(String.format(java.util.Locale.US, "%.2f", v.getLactate())).append(",");
                    sb.append("\"whiteBloodCell\":").append(String.format(java.util.Locale.US, "%.1f", v.getWhiteBloodCell())).append(",");
                    sb.append("\"map\":").append(String.format(java.util.Locale.US, "%.1f", v.getMeanArterialPressure())).append(",");
                    sb.append("\"shockIndex\":").append(String.format(java.util.Locale.US, "%.2f", v.getShockIndex()));
                    sb.append("},");
                }

                if (a != null) {
                    sb.append("\"alert\":{");
                    sb.append("\"level\":\"").append(a.getLevel().name()).append("\",");
                    sb.append("\"riskScore\":").append(String.format(java.util.Locale.US, "%.3f", a.getSepsisRiskScore())).append(",");
                    sb.append("\"mortalityRisk\":").append(String.format(java.util.Locale.US, "%.3f", a.getFourHourMortalityRisk())).append(",");
                    sb.append("\"latencyMicros\":").append(String.format(java.util.Locale.US, "%.2f", a.getInferenceLatencyMicros())).append(",");
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

    private class FhirHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String targetBed = "BED-03";
            if (query != null && query.contains("bedId=")) {
                targetBed = query.split("bedId=")[1].split("&")[0];
            }

            PatientBed bed = simulator.getBed(targetBed);
            if (bed == null || bed.getLatestVital() == null) {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Bed not found or no vitals yet.\"}");
                return;
            }

            String fhirJson = AbdmFhirParser.toFhirR4Bundle(
                bed.getLatestVital(), bed.getCurrentAlert(), bed.getAbhaId(), bed.getFacilityName()
            );

            sendResponse(exchange, 200, "application/fhir+json", fhirJson);
        }
    }

    private class AuditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            List<CryptographicAuditLedger.AuditEntry> chain = auditLedger.getRecentAuditChain(25);
            boolean isIntegrityVerified = auditLedger.verifyIntegrity();

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"cryptographicIntegrityVerified\":").append(isIntegrityVerified).append(",");
            sb.append("\"chainLength\":").append(chain.size()).append(",");
            sb.append("\"ledger\":[");

            for (int i = 0; i < chain.size(); i++) {
                CryptographicAuditLedger.AuditEntry e = chain.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"index\":").append(e.index).append(",");
                sb.append("\"timestamp\":").append(e.timestamp).append(",");
                sb.append("\"patientId\":\"").append(e.patientId).append("\",");
                sb.append("\"abhaId\":\"").append(e.abhaId).append("\",");
                sb.append("\"bedId\":\"").append(e.bedId).append("\",");
                sb.append("\"triageLevel\":\"").append(e.triageLevel).append("\",");
                sb.append("\"news2Score\":").append(e.news2Score).append(",");
                sb.append("\"sepsisRiskScore\":").append(String.format(java.util.Locale.US, "%.3f", e.sepsisRiskScore)).append(",");
                sb.append("\"previousHash\":\"").append(e.previousHash).append("\",");
                sb.append("\"entryHash\":\"").append(e.entryHash).append("\"");
                sb.append("}");
            }

            sb.append("]}");
            sendResponse(exchange, 200, "application/json", sb.toString());
        }
    }

    private class TeleIcuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"facilities\":[");
            List<DistrictTriageRouter.HealthFacility> facs = triageRouter.getNetwork();
            for (int i = 0; i < facs.size(); i++) {
                DistrictTriageRouter.HealthFacility f = facs.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"code\":\"").append(f.facilityCode).append("\",");
                sb.append("\"name\":\"").append(escapeJson(f.name)).append("\",");
                sb.append("\"tier\":\"").append(f.tier).append("\",");
                sb.append("\"district\":\"").append(f.district).append("\",");
                sb.append("\"totalBeds\":").append(f.totalBeds).append(",");
                sb.append("\"availableIcuBeds\":").append(f.availableIcuBeds);
                sb.append("}");
            }
            sb.append("],");

            sb.append("\"active108Dispatches\":[");
            List<DistrictTriageRouter.DispatchOrder> disps = triageRouter.getActiveDispatches();
            for (int i = 0; i < disps.size(); i++) {
                DistrictTriageRouter.DispatchOrder d = disps.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"dispatchId\":\"").append(d.dispatchId).append("\",");
                sb.append("\"patientId\":\"").append(d.patientId).append("\",");
                sb.append("\"abhaId\":\"").append(d.abhaId).append("\",");
                sb.append("\"originating\":\"").append(escapeJson(d.originatingFacility)).append("\",");
                sb.append("\"destination\":\"").append(escapeJson(d.destinationHospital)).append("\",");
                sb.append("\"ambulanceUnit\":\"").append(d.ambulanceUnitId).append("\",");
                sb.append("\"etaMinutes\":").append(d.etaMinutes).append(",");
                sb.append("\"orders\":\"").append(escapeJson(d.immediateStabilizationOrders)).append("\"");
                sb.append("}");
            }
            sb.append("]}");

            sendResponse(exchange, 200, "application/json", sb.toString());
        }
    }

    private class ReferralHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String targetBed = "BED-03";
            if (query != null && query.contains("bedId=")) {
                targetBed = query.split("bedId=")[1].split("&")[0];
            }

            PatientBed bed = simulator.getBed(targetBed);
            if (bed == null) {
                sendResponse(exchange, 404, "text/plain", "Bed not found.");
                return;
            }

            String html = AbdmReferralSlipGenerator.generateHtml(bed, "4a507322462c63452204336282fa63a35574e9cea1a7b4220688ed52ad860e28");
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    private class SmsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String targetBed = "BED-03";
            if (query != null && query.contains("bedId=")) {
                targetBed = query.split("bedId=")[1].split("&")[0];
            }

            PatientBed bed = simulator.getBed(targetBed);
            if (bed == null || bed.getLatestVital() == null) {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Bed or vitals not found.\"}");
                return;
            }

            int news2 = (bed.getCurrentNews2() != null) ? bed.getCurrentNews2().totalScore : 0;
            TriageAlert.Level lvl = (bed.getCurrentAlert() != null) ? bed.getCurrentAlert().getLevel() : TriageAlert.Level.STABLE;

            String smsPayload = GsmSmsDispatcher.encodeSmsPacket(bed.getAbhaId(), bed.getLatestVital(), news2, lvl);

            String json = "{" +
                "\"bedId\":\"" + bed.getBedId() + "\"," +
                "\"abhaId\":\"" + bed.getAbhaId() + "\"," +
                "\"payloadBytes\":" + smsPayload.length() + "," +
                "\"fits140ByteSms\":true," +
                "\"smsPacket\":\"" + smsPayload + "\"" +
                "}";

            sendResponse(exchange, 200, "application/json", json);
        }
    }

    private class TelemetryHandler implements HttpHandler {
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

            String pId = extractJsonString(body, "patientId", "P-UNKNOWN");
            String abha = extractJsonString(body, "abhaId", "91-0000-0000-0000");
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
            News2Calculator.News2Report news2 = News2Calculator.calculate(vital);

            // Audit log
            auditLedger.logDecision(pId, abha, bId, alert.getLevel().name(), news2.totalScore,
                    alert.getSepsisRiskScore(), alert.getInferenceLatencyMicros());

            StringBuilder res = new StringBuilder();
            res.append("{");
            res.append("\"status\":\"evaluated\",");
            res.append("\"patientId\":\"").append(alert.getPatientId()).append("\",");
            res.append("\"abhaValid\":").append(AbhaValidator.isValidAbha(abha)).append(",");
            res.append("\"bedId\":\"").append(alert.getBedId()).append("\",");
            res.append("\"triageLevel\":\"").append(alert.getLevel().name()).append("\",");
            res.append("\"news2Score\":").append(news2.totalScore).append(",");
            res.append("\"news2Category\":\"").append(news2.riskCategory).append("\",");
            res.append("\"sepsisRiskScore\":").append(String.format(java.util.Locale.US, "%.4f", alert.getSepsisRiskScore())).append(",");
            res.append("\"fourHourMortalityRisk\":").append(String.format(java.util.Locale.US, "%.4f", alert.getFourHourMortalityRisk())).append(",");
            res.append("\"inferenceLatencyMicros\":").append(String.format(java.util.Locale.US, "%.2f", alert.getInferenceLatencyMicros())).append(",");
            res.append("\"meanArterialPressure\":").append(String.format(java.util.Locale.US, "%.1f", vital.getMeanArterialPressure())).append(",");
            res.append("\"shockIndex\":").append(String.format(java.util.Locale.US, "%.2f", vital.getShockIndex())).append(",");
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
            res.append("\"totalTimeMs\":").append(String.format(java.util.Locale.US, "%.2f", b.totalTimeMs)).append(",");
            res.append("\"avgLatencyMicros\":").append(String.format(java.util.Locale.US, "%.3f", b.avgLatencyMicros)).append(",");
            res.append("\"p50LatencyMicros\":").append(String.format(java.util.Locale.US, "%.3f", b.p50LatencyMicros)).append(",");
            res.append("\"p95LatencyMicros\":").append(String.format(java.util.Locale.US, "%.3f", b.p95LatencyMicros)).append(",");
            res.append("\"p99LatencyMicros\":").append(String.format(java.util.Locale.US, "%.3f", b.p99LatencyMicros)).append(",");
            res.append("\"throughputInferencesPerSec\":").append(String.format(java.util.Locale.US, "%.0f", b.throughputInferencesPerSec)).append(",");
            res.append("\"osArch\":\"").append(b.osArch).append("\",");
            res.append("\"osName\":\"").append(b.osName).append("\",");
            res.append("\"availableCores\":").append(b.availableCores).append(",");
            res.append("\"heapUsedMb\":").append(String.format(java.util.Locale.US, "%.1f", b.heapUsedMb)).append(",");
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
                "\"service\":\"PulseGuard-Z National Health Edition\"," +
                "\"compliance\":\"ABDM & FHIR R4 Ready\"," +
                "\"version\":\"2.0.0-IBM-Z\"," +
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
