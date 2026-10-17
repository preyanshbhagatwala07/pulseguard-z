package com.ibmz.pulseguard.abdm;

import com.ibmz.pulseguard.model.PatientVital;
import com.ibmz.pulseguard.model.TriageAlert;

/**
 * AbdmFhirParser: Implements HL7 FHIR R4 (Fast Healthcare Interoperability Resources)
 * serialization and deserialization for ABDM integration.
 * Uses official LOINC and SNOMED-CT international clinical terminologies.
 */
public class AbdmFhirParser {

    /**
     * Converts a PatientVital and its TriageAlert into an authentic HL7 FHIR R4 Bundle
     * containing Observation resources, Patient reference, and an emergency DiagnosticReport.
     */
    public static String toFhirR4Bundle(PatientVital v, TriageAlert a, String abhaId, String facilityName) {
        long now = System.currentTimeMillis();
        String bundleId = "bundle-abdm-" + v.getPatientId() + "-" + now;

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"resourceType\": \"Bundle\",\n");
        sb.append("  \"id\": \"").append(bundleId).append("\",\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"versionId\": \"1\",\n");
        sb.append("    \"lastUpdated\": \"").append(new java.util.Date(now).toInstant().toString()).append("\",\n");
        sb.append("    \"profile\": [\"https://nrces.in/ndhm/fhir/r4/StructureDefinition/ObservationBundle\"]\n");
        sb.append("  },\n");
        sb.append("  \"type\": \"document\",\n");
        sb.append("  \"timestamp\": \"").append(new java.util.Date(now).toInstant().toString()).append("\",\n");
        sb.append("  \"entry\": [\n");

        // 1. Patient Resource with ABHA
        sb.append("    {\n");
        sb.append("      \"fullUrl\": \"urn:uuid:patient-").append(v.getPatientId()).append("\",\n");
        sb.append("      \"resource\": {\n");
        sb.append("        \"resourceType\": \"Patient\",\n");
        sb.append("        \"id\": \"").append(v.getPatientId()).append("\",\n");
        sb.append("        \"identifier\": [\n");
        sb.append("          {\n");
        sb.append("            \"system\": \"https://healthid.ndhm.gov.in\",\n");
        sb.append("            \"value\": \"").append(abhaId).append("\"\n");
        sb.append("          }\n");
        sb.append("        ],\n");
        sb.append("        \"managingOrganization\": { \"display\": \"").append(facilityName).append("\" }\n");
        sb.append("      }\n");
        sb.append("    },\n");

        // 2. Heart Rate Observation (LOINC 8867-4)
        sb.append(buildObservationEntry("obs-hr", "8867-4", "Heart rate", v.getHeartRate(), "/min", "bpm"));
        sb.append(",\n");

        // 3. Blood Pressure Observation (LOINC 85354-9)
        sb.append(buildBpObservationEntry("obs-bp", v.getSystolicBp(), v.getDiastolicBp()));
        sb.append(",\n");

        // 4. Oxygen Saturation (LOINC 2708-6)
        sb.append(buildObservationEntry("obs-spo2", "2708-6", "Oxygen saturation in Arterial blood by Pulse oximetry", v.getSpO2(), "%", "%"));
        sb.append(",\n");

        // 5. Serum Lactate (LOINC 2524-7)
        sb.append(buildObservationEntry("obs-lactate", "2524-7", "Lactate [Moles/volume] in Blood", v.getLactate(), "mmol/L", "mmol/L"));
        sb.append(",\n");

        // 6. Clinical Sepsis Triage DiagnosticReport
        sb.append("    {\n");
        sb.append("      \"fullUrl\": \"urn:uuid:report-triage-").append(v.getPatientId()).append("\",\n");
        sb.append("      \"resource\": {\n");
        sb.append("        \"resourceType\": \"DiagnosticReport\",\n");
        sb.append("        \"id\": \"triage-").append(v.getPatientId()).append("\",\n");
        sb.append("        \"status\": \"final\",\n");
        sb.append("        \"category\": [{ \"coding\": [{ \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0074\", \"code\": \"GE\", \"display\": \"General\" }] }],\n");
        sb.append("        \"code\": { \"coding\": [{ \"system\": \"http://snomed.info/sct\", \"code\": \"414027003\", \"display\": \"Critical Care Triage Risk Assessment\" }] },\n");
        sb.append("        \"conclusion\": \"").append(escapeJson(a.getClinicalRecommendation())).append("\",\n");
        sb.append("        \"conclusionCode\": [{ \"coding\": [{ \"system\": \"http://snomed.info/sct\", \"code\": \"422504002\", \"display\": \"").append(a.getLevel().name()).append("\" }] }]\n");
        sb.append("      }\n");
        sb.append("    }\n");

        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static String buildObservationEntry(String id, String loincCode, String display, double value, String unit, String code) {
        return "    {\n" +
               "      \"fullUrl\": \"urn:uuid:" + id + "\",\n" +
               "      \"resource\": {\n" +
               "        \"resourceType\": \"Observation\",\n" +
               "        \"id\": \"" + id + "\",\n" +
               "        \"status\": \"final\",\n" +
               "        \"code\": { \"coding\": [{ \"system\": \"http://loinc.org\", \"code\": \"" + loincCode + "\", \"display\": \"" + display + "\" }] },\n" +
               "        \"valueQuantity\": { \"value\": " + String.format(java.util.Locale.US, "%.1f", value) + ", \"unit\": \"" + unit + "\", \"system\": \"http://unitsofmeasure.org\", \"code\": \"" + code + "\" }\n" +
               "      }\n" +
               "    }";
    }

    private static String buildBpObservationEntry(String id, double sbp, double dbp) {
        return "    {\n" +
               "      \"fullUrl\": \"urn:uuid:" + id + "\",\n" +
               "      \"resource\": {\n" +
               "        \"resourceType\": \"Observation\",\n" +
               "        \"id\": \"" + id + "\",\n" +
               "        \"status\": \"final\",\n" +
               "        \"code\": { \"coding\": [{ \"system\": \"http://loinc.org\", \"code\": \"85354-9\", \"display\": \"Blood pressure panel\" }] },\n" +
               "        \"component\": [\n" +
               "          {\n" +
               "            \"code\": { \"coding\": [{ \"system\": \"http://loinc.org\", \"code\": \"8480-6\", \"display\": \"Systolic blood pressure\" }] },\n" +
               "            \"valueQuantity\": { \"value\": " + String.format(java.util.Locale.US, "%.1f", sbp) + ", \"unit\": \"mmHg\", \"system\": \"http://unitsofmeasure.org\", \"code\": \"mm[Hg]\" }\n" +
               "          },\n" +
               "          {\n" +
               "            \"code\": { \"coding\": [{ \"system\": \"http://loinc.org\", \"code\": \"8462-4\", \"display\": \"Diastolic blood pressure\" }] },\n" +
               "            \"valueQuantity\": { \"value\": " + String.format(java.util.Locale.US, "%.1f", dbp) + ", \"unit\": \"mmHg\", \"system\": \"http://unitsofmeasure.org\", \"code\": \"mm[Hg]\" }\n" +
               "          }\n" +
               "        ]\n" +
               "      }\n" +
               "    }";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
