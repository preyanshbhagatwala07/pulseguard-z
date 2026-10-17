package com.ibmz.pulseguard.abdm;

import java.util.regex.Pattern;

/**
 * AbhaValidator: Implements National Health Authority (NHA) & Ayushman Bharat Digital Mission (ABDM)
 * validation rules for ABHA (Ayushman Bharat Health Account) IDs and ABHA Addresses.
 */
public class AbhaValidator {

    // 14-digit ABHA Number format: XX-XXXX-XXXX-XXXX
    private static final Pattern ABHA_NUMBER_PATTERN = Pattern.compile("^\\d{2}-\\d{4}-\\d{4}-\\d{4}$");

    // ABHA Address format: alphanumeric with dots/underscores + @abdm (e.g. preyansh.b@abdm)
    private static final Pattern ABHA_ADDRESS_PATTERN = Pattern.compile("^[a-zA-Z0-9._]{3,32}@(abdm|sbx)$");

    public static boolean isValidAbha(String id) {
        if (id == null || id.isBlank()) return false;
        id = id.trim();
        return ABHA_NUMBER_PATTERN.matcher(id).matches() || ABHA_ADDRESS_PATTERN.matcher(id).matches();
    }

    public static String formatAbha(String raw) {
        if (raw == null) return "N/A";
        raw = raw.trim();
        if (raw.length() == 14 && raw.matches("^\\d{14}$")) {
            return raw.substring(0, 2) + "-" + raw.substring(2, 6) + "-" + raw.substring(6, 10) + "-" + raw.substring(10, 14);
        }
        return raw;
    }
}
