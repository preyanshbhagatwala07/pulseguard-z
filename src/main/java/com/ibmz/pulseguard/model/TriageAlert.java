package com.ibmz.pulseguard.model;

import java.util.List;

/**
 * TriageAlert encapsulates the real-time AI decision output for a patient vital packet.
 */
public class TriageAlert {
    public enum Level {
        STABLE,
        ELEVATED_RISK,
        CRITICAL_SEPSIS
    }

    private final String patientId;
    private final String bedId;
    private final Level level;
    private final double sepsisRiskScore;       // 0.00 to 1.00 (probability)
    private final double fourHourMortalityRisk; // 0.00 to 1.00
    private final double inferenceLatencyMicros;// Microseconds taken for decision
    private final List<String> contributingBiomarkers;
    private final String clinicalRecommendation;
    private final long timestamp;

    public TriageAlert(String patientId, String bedId, Level level,
                       double sepsisRiskScore, double fourHourMortalityRisk,
                       double inferenceLatencyMicros,
                       List<String> contributingBiomarkers,
                       String clinicalRecommendation, long timestamp) {
        this.patientId = patientId;
        this.bedId = bedId;
        this.level = level;
        this.sepsisRiskScore = sepsisRiskScore;
        this.fourHourMortalityRisk = fourHourMortalityRisk;
        this.inferenceLatencyMicros = inferenceLatencyMicros;
        this.contributingBiomarkers = contributingBiomarkers;
        this.clinicalRecommendation = clinicalRecommendation;
        this.timestamp = timestamp;
    }

    public String getPatientId() { return patientId; }
    public String getBedId() { return bedId; }
    public Level getLevel() { return level; }
    public double getSepsisRiskScore() { return sepsisRiskScore; }
    public double getFourHourMortalityRisk() { return fourHourMortalityRisk; }
    public double getInferenceLatencyMicros() { return inferenceLatencyMicros; }
    public List<String> getContributingBiomarkers() { return contributingBiomarkers; }
    public String getClinicalRecommendation() { return clinicalRecommendation; }
    public long getTimestamp() { return timestamp; }
}
