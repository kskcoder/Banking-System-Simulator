package com.tejas.bankcardservice.utils;

import org.springframework.stereotype.Component;

@Component
public class CardMask {
    public static String maskCardNumber(String rawCardNumber, boolean lastFour) {
    	
    	if (lastFour) {
    		return "XXXX XXXX XXXX " + rawCardNumber;
    	}
    	
        if (rawCardNumber == null || rawCardNumber.length() != 16) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }

        String last4 = rawCardNumber.substring(12);
        return "XXXX XXXX XXXX " + last4;
    }

    public static String formatCardNumber(String rawCardNumber) {
        if (rawCardNumber == null || rawCardNumber.length() != 16) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }

        return rawCardNumber.replaceAll("(.{4})(?!$)", "$1 ");
    }
}
