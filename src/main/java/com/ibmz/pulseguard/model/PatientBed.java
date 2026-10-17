package com.ibmz.pulseguard.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates the ongoing state, history, and current AI alert of an ICU bed.
 */
public class PatientBed {
    private final String bedId;
    private final String patientId;
    private final String patientName;
    private final int age;
    private final String primaryDiagnosis;
    private final LinkedList<PatientVital> vitalHistory = new LinkedList<>();
    private final LinkedList<TriageAlert> alertHistory = new LinkedList<>();
    private TriageAlert currentAlert;

    public PatientBed(String bedId, String patientId, String patientName, int age, String primaryDiagnosis) {
        this.bedId = bedId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.age = age;
        this.primaryDiagnosis = primaryDiagnosis;
    }

    public synchronized void recordVitalAndAlert(PatientVital vital, TriageAlert alert) {
        vitalHistory.addLast(vital);
        if (vitalHistory.size() > 60) {
            vitalHistory.removeFirst();
        }
        this.currentAlert = alert;
        alertHistory.addLast(alert);
        if (alertHistory.size() > 60) {
            alertHistory.removeFirst();
        }
    }

    public String getBedId() { return bedId; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public int getAge() { return age; }
    public String getPrimaryDiagnosis() { return primaryDiagnosis; }
    public synchronized PatientVital getLatestVital() { return vitalHistory.isEmpty() ? null : vitalHistory.getLast(); }
    public synchronized TriageAlert getCurrentAlert() { return currentAlert; }
    public synchronized List<PatientVital> getVitalHistory() { return new LinkedList<>(vitalHistory); }
    public synchronized List<TriageAlert> getAlertHistory() { return new LinkedList<>(alertHistory); }
}
