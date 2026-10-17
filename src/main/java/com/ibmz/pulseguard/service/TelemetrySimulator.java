package com.ibmz.pulseguard.service;

import com.ibmz.pulseguard.audit.CryptographicAuditLedger;
import com.ibmz.pulseguard.clinical.News2Calculator;
import com.ibmz.pulseguard.engine.SepsisRiskEngine;
import com.ibmz.pulseguard.model.PatientBed;
import com.ibmz.pulseguard.model.PatientVital;
import com.ibmz.pulseguard.model.TriageAlert;
import com.ibmz.pulseguard.teleicu.DistrictTriageRouter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TelemetrySimulator: Realistic ICU telemetry simulator integrating ABDM ABHA IDs,
 * district healthcare facilities, and automated audit logging.
 */
public class TelemetrySimulator {

    private final List<PatientBed> beds = new ArrayList<>();
    private final Random rand = new Random(42);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final CryptographicAuditLedger auditLedger;
    private final DistrictTriageRouter triageRouter;

    public TelemetrySimulator(CryptographicAuditLedger auditLedger, DistrictTriageRouter triageRouter) {
        this.auditLedger = auditLedger;
        this.triageRouter = triageRouter;

        // Initialize 6 authentic hospital profiles across district hubs and rural PHCs
        beds.add(new PatientBed("BED-01", "P-101", "91-2384-9102-4821", "Rameshwar Patel", 58, "Male",
                "Civil Hospital Valsad (District HQ)", "Post-Op Abdominal Surgery"));

        beds.add(new PatientBed("BED-02", "P-102", "91-7291-8291-0192", "Sunita Sharma", 46, "Female",
                "Kaprada Community Health Centre (CHC)", "Severe Scrub Typhus & High Fever"));

        beds.add(new PatientBed("BED-03", "P-103", "91-1049-5829-3910", "Mohammad Farooq", 68, "Male",
                "Civil Hospital Valsad (ICU Bay 3)", "Severe Urosepsis & Septic Shock Decompensation"));

        beds.add(new PatientBed("BED-04", "P-104", "91-8392-1049-5729", "Pooja Verma", 29, "Female",
                "Dharampur Rural PHC (Triage Bed 1)", "Postpartum Infection & Occult Shock"));

        beds.add(new PatientBed("BED-05", "P-105", "91-5920-1940-2850", "Anand Kulkarni", 62, "Male",
                "Civil Hospital Surat (HDU)", "Acute Necrotizing Pancreatitis (SIRS)"));

        beds.add(new PatientBed("BED-06", "P-106", "91-4920-1049-2810", "Kavita Devi", 51, "Female",
                "AIIMS Apex Trauma & Critical Care", "Septic Shock (Post-Antibiotic Day 2 Recovery)"));

        // Seed initial history
        for (int step = 0; step < 15; step++) {
            tick();
        }

        // Run scheduled live streaming updates every 1500 ms
        scheduler.scheduleAtFixedRate(this::tick, 1500, 1500, TimeUnit.MILLISECONDS);
    }

    public synchronized void tick() {
        long now = System.currentTimeMillis();

        for (int i = 0; i < beds.size(); i++) {
            PatientBed bed = beds.get(i);
            PatientVital vital = generateVital(i, bed.getPatientId(), bed.getBedId(), now);
            TriageAlert alert = SepsisRiskEngine.evaluate(vital);
            News2Calculator.News2Report news2 = News2Calculator.calculate(vital);

            bed.recordVitalAndAlert(vital, alert, news2);

            // Cryptographically log critical and elevated events for legal audit trail
            if (alert.getLevel() != TriageAlert.Level.STABLE || news2.totalScore >= 5) {
                auditLedger.logDecision(bed.getPatientId(), bed.getAbhaId(), bed.getBedId(),
                        alert.getLevel().name(), news2.totalScore, alert.getSepsisRiskScore(), alert.getInferenceLatencyMicros());
            }

            // Auto-trigger Emergency 108 Dispatch for rural PHC patients in critical shock
            if (i == 3 && alert.getLevel() == TriageAlert.Level.CRITICAL_SEPSIS && triageRouter.getActiveDispatches().isEmpty()) {
                triageRouter.triggerEmergencyTransfer(bed.getPatientId(), bed.getAbhaId(), bed.getFacilityName(),
                        news2.totalScore, alert.getSepsisRiskScore(), "Shock Index > 1.25, MAP < 60");
            }
        }
    }

    private PatientVital generateVital(int bedIndex, String patientId, String bedId, long timestamp) {
        double hr, sbp, dbp, rr, temp, spo2, lactate, wbc;
        double noise = rand.nextGaussian();

        switch (bedIndex) {
            case 0: // Bed 1: Stable post-op
                hr = 74 + noise * 3;
                sbp = 118 + noise * 4;
                dbp = 76 + noise * 3;
                rr = 15 + noise * 1;
                temp = 36.8 + noise * 0.1;
                spo2 = 98 + noise * 0.5;
                lactate = 1.1 + noise * 0.1;
                wbc = 7.8 + noise * 0.3;
                break;

            case 1: // Bed 2: Scrub Typhus with elevated risk
                hr = 101 + noise * 4;
                sbp = 102 + noise * 5;
                dbp = 64 + noise * 3;
                rr = 23 + noise * 1.5;
                temp = 38.6 + noise * 0.2;
                spo2 = 93.0 + noise * 1.0;
                lactate = 2.5 + noise * 0.2;
                wbc = 14.1 + noise * 0.5;
                break;

            case 2: // Bed 3: Acute Septic Shock Decompensation (CRITICAL!)
                hr = 129 + noise * 5;
                sbp = 80 + noise * 4;
                dbp = 45 + noise * 3;
                rr = 29 + noise * 2;
                temp = 39.3 + noise * 0.3;
                spo2 = 88.0 + noise * 1.2;
                lactate = 4.9 + noise * 0.3;
                wbc = 20.4 + noise * 0.7;
                break;

            case 3: // Bed 4: Rural PHC Postpartum Sepsis (Critical Alert Trigger)
                hr = 124 + noise * 4;
                sbp = 84 + noise * 4;
                dbp = 48 + noise * 3;
                rr = 27 + noise * 1.8;
                temp = 39.1 + noise * 0.2;
                spo2 = 89.5 + noise * 1.0;
                lactate = 4.4 + noise * 0.25;
                wbc = 18.2 + noise * 0.6;
                break;

            case 4: // Bed 5: Pancreatitis SIRS
                hr = 104 + noise * 4;
                sbp = 96 + noise * 5;
                dbp = 60 + noise * 3;
                rr = 24 + noise * 1.5;
                temp = 38.8 + noise * 0.2;
                spo2 = 92.5 + noise * 0.8;
                lactate = 2.9 + noise * 0.2;
                wbc = 15.8 + noise * 0.6;
                break;

            case 5: // Bed 6: Resolving sepsis, recovering
            default:
                hr = 82 + noise * 3;
                sbp = 114 + noise * 4;
                dbp = 72 + noise * 3;
                rr = 16 + noise * 1;
                temp = 37.1 + noise * 0.15;
                spo2 = 97.0 + noise * 0.5;
                lactate = 1.5 + noise * 0.15;
                wbc = 9.4 + noise * 0.4;
                break;
        }

        // Realistic physiological bounds
        spo2 = Math.min(100.0, Math.max(70.0, spo2));
        hr = Math.max(40.0, hr);
        sbp = Math.max(50.0, sbp);
        dbp = Math.max(30.0, dbp);
        lactate = Math.max(0.4, lactate);

        return new PatientVital(patientId, bedId, timestamp, hr, sbp, dbp, rr, temp, spo2, lactate, wbc);
    }

    public synchronized List<PatientBed> getBeds() {
        return Collections.unmodifiableList(beds);
    }

    public synchronized PatientBed getBed(String bedId) {
        for (PatientBed b : beds) {
            if (b.getBedId().equalsIgnoreCase(bedId)) {
                return b;
            }
        }
        return null;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
