package com.ibmz.pulseguard.model;

import com.ibmz.pulseguard.clinical.News2Calculator;

import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates the ongoing state, history, ABHA identity, and current AI alert of an ICU bed.
 */
public class PatientBed {
    private final String bedId;
    private final String patientId;
    private final String abhaId;
    private final String patientName;
    private final int age;
    private final String gender;
    private final String facilityName;
    private final String primaryDiagnosis;
    private final LinkedList<PatientVital> vitalHistory = new LinkedList<>();
    private final LinkedList<TriageAlert> alertHistory = new LinkedList<>();
    private TriageAlert currentAlert;
    private News2Calculator.News2Report currentNews2;

    public PatientBed(String bedId, String patientId, String abhaId, String patientName,
                      int age, String gender, String facilityName, String primaryDiagnosis) {
        this.bedId = bedId;
        this.patientId = patientId;
        this.abhaId = abhaId;
        this.patientName = patientName;
        this.age = age;
        this.gender = gender;
        this.facilityName = facilityName;
        this.primaryDiagnosis = primaryDiagnosis;
    }

    public synchronized void recordVitalAndAlert(PatientVital vital, TriageAlert alert, News2Calculator.News2Report news2) {
        vitalHistory.addLast(vital);
        if (vitalHistory.size() > 60) {
            vitalHistory.removeFirst();
        }
        this.currentAlert = alert;
        this.currentNews2 = news2;
        alertHistory.addLast(alert);
        if (alertHistory.size() > 60) {
            alertHistory.removeFirst();
        }
    }

    public String getBedId() { return bedId; }
    public String getPatientId() { return patientId; }
    public String getAbhaId() { return abhaId; }
    public String getPatientName() { return patientName; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getFacilityName() { return facilityName; }
    public String getPrimaryDiagnosis() { return primaryDiagnosis; }
    public synchronized PatientVital getLatestVital() { return vitalHistory.isEmpty() ? null : vitalHistory.getLast(); }
    public synchronized TriageAlert getCurrentAlert() { return currentAlert; }
    public synchronized News2Calculator.News2Report getCurrentNews2() { return currentNews2; }
    public synchronized List<PatientVital> getVitalHistory() { return new LinkedList<>(vitalHistory); }
    public synchronized List<TriageAlert> getAlertHistory() { return new LinkedList<>(alertHistory); }
}
