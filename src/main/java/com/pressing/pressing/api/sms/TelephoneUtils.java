package com.pressing.pressing.api.sms;

public final class TelephoneUtils {

    private TelephoneUtils() {
    }

    public static String normaliser(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            return null;
        }

        String t = telephone.replaceAll("[\\s\\-.()/]", "");
        if (t.startsWith("+")) {
            t = t.substring(1);
        }
        if (t.startsWith("00")) {
            t = t.substring(2);
        }
        if (t.startsWith("237")) {
            t = t.substring(3);
        }
        if (t.startsWith("0")) {
            t = t.substring(1);
        }
        if (t.length() != 9) {
            return null;
        }
        return "+237" + t;
    }
}