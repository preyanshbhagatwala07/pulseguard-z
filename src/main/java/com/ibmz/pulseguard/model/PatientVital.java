package com.ibmz.pulseguard.model;

/**
 * Represents a high-frequency physiological telemetry sample from an ICU bedside monitor.
 */
public class PatientVital {
    private final String patientId;
    private final String bedId;
    private final long timestamp;
    private final double heartRate;      // bpm (normal: 60-100)
    private final double systolicBp;     // mmHg (normal: 90-120)
    private final double diastolicBp;    // mmHg (normal: 60-80)
    private final double respRate;       // breaths/min (normal: 12-20)
    private final double temperatureC;   // Celsius (normal: 36.5 - 37.5)
    private final double spO2;           // % (normal: 95-100)
    private final double lactate;        // mmol/L (normal: 0.5 - 2.0; > 2.0 warning, > 4.0 septic shock)
    private final double whiteBloodCell; // x10^9/L (normal: 4.5 - 11.0)

    public PatientVital(String patientId, String bedId, long timestamp,
                        double heartRate, double systolicBp, double diastolicBp,
                        double respRate, double temperatureC, double spO2,
                        double lactate, double whiteBloodCell) {
        this.patientId = patientId;
        this.bedId = bedId;
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.respRate = respRate;
        this.temperatureC = temperatureC;
        this.spO2 = spO2;
        this.lactate = lactate;
        this.whiteBloodCell = whiteBloodCell;
    }

    public String getPatientId() { return patientId; }
    public String getBedId() { return bedId; }
    public long getTimestamp() { return timestamp; }
    public double getHeartRate() { return heartRate; }
    public double getSystolicBp() { return systolicBp; }
    public double getDiastolicBp() { return diastolicBp; }
    public double getRespRate() { return respRate; }
    public double getTemperatureC() { return temperatureC; }
    public double getSpO2() { return spO2; }
    public double getLactate() { return lactate; }
    public double getWhiteBloodCell() { return whiteBloodCell; }

    /** Mean Arterial Pressure (MAP) = (SBP + 2 * DBP) / 3 */
    public double getMeanArterialPressure() {
        return (systolicBp + 2.0 * diastolicBp) / 3.0;
    }

    /** Shock Index (SI) = HR / SBP. Normal: 0.5 - 0.7. Value > 0.9 strongly indicates occult shock. */
    public double getShockIndex() {
        return systolicBp > 0 ? (heartRate / systolicBp) : 0.0;
    }

    /** Quick SOFA (qSOFA) score approximation: RR >= 22 (+1), SBP <= 100 (+1) */
    public int getQsofaScore() {
        int score = 0;
        if (respRate >= 22.0) score++;
        if (systolicBp <= 100.0) score++;
        if (heartRate >= 100.0) score++;
        return score;
    }
}
