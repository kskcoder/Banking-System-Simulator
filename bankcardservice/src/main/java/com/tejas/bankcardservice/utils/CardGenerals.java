package com.tejas.bankcardservice.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class CardGenerals {
	public String cardNumberGenerator() {
	    Random random = new Random();
	    StringBuilder number = new StringBuilder("1111");

	    for (int i = 0; i < 12; i++) {
	        number.append(random.nextInt(10));
	    }
	    return number.toString();
	}
	
	public String cvvGenerator() {
	    Random random = new Random();
	    return String.format("%03d", random.nextInt(1000));
	}
	
	public String hash(String data) {
	    return DigestUtils.sha256Hex(data);
	}
	
	public String expiryGenerator() {
	    LocalDate expiry = LocalDate.now().plusYears(5);
	    return expiry.format(DateTimeFormatter.ofPattern("MM/yyyy"));
	}
	
	public boolean isExpired(String expiryString) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

	    YearMonth expiry = YearMonth.parse(expiryString, formatter);

	    LocalDate expiryDate = expiry.atEndOfMonth();

	    LocalDate today = LocalDate.now();

	    return today.isAfter(expiryDate);
	}
}
