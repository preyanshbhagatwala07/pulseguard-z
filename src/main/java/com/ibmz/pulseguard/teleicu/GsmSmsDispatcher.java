package com.ibmz.pulseguard.teleicu;

import com.ibmz.pulseguard.model.PatientVital;
import com.ibmz.pulseguard.model.TriageAlert;

/**
 * GsmSmsDispatcher: Offline-Resilient Low-Bandwidth SMS Protocol.
 * Transmits critical triage packets over 2G / GSM / USSD when internet connectivity drops
 * at rural Primary Health Centres (PHCs). Fits under standard 140-byte SMS constraints.
 */
public class GsmSmsDispatcher {

    /**
     * Compresses clinical vitals into an ultra-dense GSM SMS payload for transmission over 2G SMS gateways.
     * Format: PZ108|<ABHA_STRIPPED>|<HR>,<SBP>,<DBP>,<RR>,<SPO2>,<LACT>|<NEWS2>|<SEV>|<CRC16>
     */
    public static String encodeSmsPacket(String abhaId, PatientVital v, int news2, TriageAlert.Level level) {
        String cleanAbha = abhaId.replaceAll("[^0-9]", "");
        String payload = String.format(java.util.Locale.US,
            "PZ108|%s|%.0f,%.0f,%.0f,%.0f,%.0f,%.1f|%d|%s",
            cleanAbha,
            v.getHeartRate(), v.getSystolicBp(), v.getDiastolicBp(),
            v.getRespRate(), v.getSpO2(), v.getLactate(),
            news2,
            level == TriageAlert.Level.CRITICAL_SEPSIS ? "CRIT" : (level == TriageAlert.Level.ELEVATED_RISK ? "WARN" : "STBL")
        );

        int crc = calculateCrc16(payload);
        return payload + "|" + String.format("%04X", crc);
    }

    private static int calculateCrc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes()) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc = (crc >>> 1);
                }
            }
        }
        return crc & 0xFFFF;
    }
}
