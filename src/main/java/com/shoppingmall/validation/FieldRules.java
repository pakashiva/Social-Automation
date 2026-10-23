package com.shoppingmall.validation;

public final class FieldRules {

    private FieldRules() {
    }

    public static boolean password(String value) {
        if (value == null || value.length() < 8 || value.length() > 72) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                letter = true;
            } else if (c >= '0' && c <= '9') {
                digit = true;
            }
        }
        return letter && digit;
    }

    public static boolean mobile(String value) {
        return value != null && value.matches("^[6-9][0-9]{9}$");
    }

    public static boolean gst(String value) {
        return value != null && value.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    }
}
