package com.ibmz.pulseguard.engine;

import com.ibmz.pulseguard.model.PatientVital;

/**
 * FeatureExtractor converts raw telemetry packets into normalized feature vectors
 * for vectorized matrix scoring on enterprise architectures (IBM Z / LinuxONE).
 */
public class FeatureExtractor {

    public static final int FEATURE_DIM = 14;

    /**
     * Extracts a normalized 14-dimensional feature vector from a PatientVital sample.
     */
    public static double[] extract(PatientVital v) {
        double[] f = new double[FEATURE_DIM];

        // 1. Normalized Heart Rate (center: 75, scale: 25)
        f[0] = (v.getHeartRate() - 75.0) / 25.0;

        // 2. Normalized Systolic BP (center: 110, scale: 20)
        f[1] = (v.getSystolicBp() - 110.0) / 20.0;

        // 3. Normalized Diastolic BP (center: 70, scale: 15)
        f[2] = (v.getDiastolicBp() - 70.0) / 15.0;

        // 4. Mean Arterial Pressure (MAP)
        double map = v.getMeanArterialPressure();
        f[3] = (map - 85.0) / 15.0;

        // 5. Shock Index (HR / SBP)
        double si = v.getShockIndex();
        f[4] = (si - 0.65) / 0.25;

        // 6. Normalized Respiratory Rate (center: 16, scale: 5)
        f[5] = (v.getRespRate() - 16.0) / 5.0;

        // 7. SpO2 Oxygen Saturation Deficit (100 - SpO2)
        double spo2Deficit = Math.max(0.0, 100.0 - v.getSpO2());
        f[6] = spo2Deficit / 5.0;

        // 8. Body Temperature deviation from norm (37.0 C)
        f[7] = Math.abs(v.getTemperatureC() - 37.0) / 1.0;

        // 9. Blood Lactate Level (normal < 2.0 mmol/L)
        f[8] = (v.getLactate() - 1.5) / 1.0;

        // 10. WBC Anomaly distance (normal 4.5-11.0)
        f[9] = (v.getWhiteBloodCell() - 7.5) / 4.0;

        // 11. High Shock Index trigger (> 0.90 is severe occult shock indicator)
        f[10] = si > 0.90 ? 1.0 : 0.0;

        // 12. Critical Hypotension trigger (MAP < 65 mmHg implies organ hypoperfusion)
        f[11] = map < 65.0 ? 1.0 : 0.0;

        // 13. Critical Hypoxia trigger (SpO2 < 90%)
        f[12] = v.getSpO2() < 90.0 ? 1.0 : 0.0;

        // 14. Severe Hyperlactatemia trigger (> 4.0 mmol/L indicates septic shock)
        f[13] = v.getLactate() > 4.0 ? 1.0 : 0.0;

        return f;
    }
}
