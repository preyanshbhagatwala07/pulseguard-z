package com.ibmz.pulseguard.service;

import com.ibmz.pulseguard.engine.SepsisRiskEngine;
import com.ibmz.pulseguard.model.PatientBed;
import com.ibmz.pulseguard.model.PatientVital;
import com.ibmz.pulseguard.model.TriageAlert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TelemetrySimulator: Simulates live bedside telemetry feeds for a 6-bed Intensive Care Unit.
 */
public class TelemetrySimulator {

    private final List<PatientBed> beds = new ArrayList<>();
    private final Random rand = new Random(42);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TelemetrySimulator() {
        // Initialize 6 diverse ICU patient profiles
        beds.add(new PatientBed("BED-01", "P-101", "John Doe", 58, "Post-Op Abdominal Surgery"));
        beds.add(new PatientBed("BED-02", "P-102", "Maria Rodriguez", 64, "Hospital-Acquired Pneumonia"));
        beds.add(new PatientBed("BED-03", "P-103", "Robert Chen", 71, "Severe Urosepsis"));
        beds.add(new PatientBed("BED-04", "P-104", "Sarah Johnson", 45, "Diabetic Ketoacidosis (Recovery)"));
        beds.add(new PatientBed("BED-05", "P-105", "James Wilson", 67, "Acute Necrotizing Pancreatitis"));
        beds.add(new PatientBed("BED-06", "P-106", "Emily Brown", 52, "Septic Shock (Post-Antibiotic Day 2)"));

        // Seed initial history
        for (int step = 0; step < 20; step++) {
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
            bed.recordVitalAndAlert(vital, alert);
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

            case 1: // Bed 2: Pneumonia with elevated risk
                hr = 98 + noise * 4;
                sbp = 104 + noise * 5;
                dbp = 66 + noise * 3;
                rr = 22 + noise * 1.5;
                temp = 38.4 + noise * 0.2;
                spo2 = 92.5 + noise * 1.0;
                lactate = 2.4 + noise * 0.2;
                wbc = 13.5 + noise * 0.5;
                break;

            case 2: // Bed 3: Acute Septic Shock Decompensation (CRITICAL!)
                hr = 126 + noise * 5;
                sbp = 82 + noise * 4;
                dbp = 46 + noise * 3;
                rr = 28 + noise * 2;
                temp = 39.2 + noise * 0.3;
                spo2 = 88.5 + noise * 1.2;
                lactate = 4.8 + noise * 0.3;
                wbc = 19.8 + noise * 0.7;
                break;

            case 3: // Bed 4: DKA recovery, stable
                hr = 80 + noise * 3;
                sbp = 115 + noise * 4;
                dbp = 74 + noise * 3;
                rr = 16 + noise * 1;
                temp = 37.0 + noise * 0.1;
                spo2 = 99 + noise * 0.4;
                lactate = 1.3 + noise * 0.1;
                wbc = 8.5 + noise * 0.3;
                break;

            case 4: // Bed 5: Pancreatitis drifting towards SIRS
                hr = 102 + noise * 4;
                sbp = 98 + noise * 5;
                dbp = 62 + noise * 3;
                rr = 23 + noise * 1.5;
                temp = 38.6 + noise * 0.2;
                spo2 = 93.0 + noise * 0.8;
                lactate = 2.8 + noise * 0.2;
                wbc = 15.2 + noise * 0.6;
                break;

            case 5: // Bed 6: Resolving sepsis, recovering
            default:
                hr = 84 + noise * 3;
                sbp = 112 + noise * 4;
                dbp = 70 + noise * 3;
                rr = 17 + noise * 1;
                temp = 37.2 + noise * 0.15;
                spo2 = 96.5 + noise * 0.6;
                lactate = 1.6 + noise * 0.15;
                wbc = 9.8 + noise * 0.4;
                break;
        }

        // Clamp values to realistic physiological bounds
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
