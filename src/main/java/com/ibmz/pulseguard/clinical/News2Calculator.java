package com.ibmz.pulseguard.clinical;

import com.ibmz.pulseguard.model.PatientVital;

/**
 * News2Calculator: Implements the Royal College of Physicians / AIIMS / NHS
 * National Early Warning Score 2 (NEWS2), the globally validated clinical score
 * for acute patient deterioration and sepsis triage.
 */
public class News2Calculator {

    public static class News2Report {
        public final int totalScore;
        public final int respScore;
        public final int spo2Score;
        public final int sbpScore;
        public final int pulseScore;
        public final int tempScore;
        public final boolean hasIndividualRedFlag; // Any single parameter scoring 3
        public final String riskCategory;         // LOW, MEDIUM, HIGH
        public final String clinicalResponse;

        public News2Report(int totalScore, int respScore, int spo2Score, int sbpScore,
                           int pulseScore, int tempScore, boolean hasIndividualRedFlag,
                           String riskCategory, String clinicalResponse) {
            this.totalScore = totalScore;
            this.respScore = respScore;
            this.spo2Score = spo2Score;
            this.sbpScore = sbpScore;
            this.pulseScore = pulseScore;
            this.tempScore = tempScore;
            this.hasIndividualRedFlag = hasIndividualRedFlag;
            this.riskCategory = riskCategory;
            this.clinicalResponse = clinicalResponse;
        }
    }

    public static News2Report calculate(PatientVital v) {
        // 1. Respiration Rate
        int resp;
        double rr = v.getRespRate();
        if (rr <= 8) resp = 3;
        else if (rr <= 11) resp = 1;
        else if (rr <= 20) resp = 0;
        else if (rr <= 24) resp = 2;
        else resp = 3;

        // 2. Oxygen Saturation (SpO2 Scale 1)
        int spo2;
        double sat = v.getSpO2();
        if (sat <= 91) spo2 = 3;
        else if (sat <= 93) spo2 = 2;
        else if (sat <= 95) spo2 = 1;
        else spo2 = 0;

        // 3. Systolic Blood Pressure
        int sbp;
        double bp = v.getSystolicBp();
        if (bp <= 90) sbp = 3;
        else if (bp <= 100) sbp = 2;
        else if (bp <= 110) sbp = 1;
        else if (bp <= 219) sbp = 0;
        else sbp = 3;

        // 4. Pulse / Heart Rate
        int pulse;
        double hr = v.getHeartRate();
        if (hr <= 40) pulse = 3;
        else if (hr <= 50) pulse = 1;
        else if (hr <= 90) pulse = 0;
        else if (hr <= 110) pulse = 1;
        else if (hr <= 130) pulse = 2;
        else pulse = 3;

        // 5. Temperature
        int temp;
        double t = v.getTemperatureC();
        if (t <= 35.0) temp = 3;
        else if (t <= 36.0) temp = 1;
        else if (t <= 38.0) temp = 0;
        else if (t <= 39.0) temp = 1;
        else temp = 2;

        int total = resp + spo2 + sbp + pulse + temp;
        boolean redFlag = (resp == 3 || spo2 == 3 || sbp == 3 || pulse == 3 || temp == 3);

        String category;
        String response;

        if (total >= 7) {
            category = "HIGH_CRITICAL";
            response = "EMERGENCY: Immediate assessment by critical care / intensivist team. Continuous vital monitoring, transfer to ICU/HDU.";
        } else if (total >= 5 || redFlag) {
            category = "MEDIUM_URGENT";
            response = "URGENT: Review by medical team within 1 hour. Escalation to clinical supervisor and increase monitoring frequency to 1 hour.";
        } else if (total >= 1) {
            category = "LOW";
            response = "STABLE: Inform registered nurse. Continue routine 4-6 hourly observations.";
        } else {
            category = "NORMAL";
            response = "NORMAL: Patient physiological parameters optimal. Routine 12-hourly ward monitoring.";
        }

        return new News2Report(total, resp, spo2, sbp, pulse, temp, redFlag, category, response);
    }
}
