package com.tejas.bankcardservice.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import com.tejas.bankcardservice.dtos.GeneralCardResponse;
import com.tejas.bankcardservice.model.Card;
import com.tejas.bankingcommon.enums.AccountType;

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
	
	public double getDailyLimit(AccountType type) {
		return type == AccountType.SAVINGS ? 50000.0 : 200000.0;
	}
	
	public GeneralCardResponse createGeneralCardResponse(Card card) {
		return GeneralCardResponse.builder()
				.expiry(card.getLastDigits())
				.maskedCardNumber(CardMask.maskCardNumber(card.getLastDigits(), true))
				.status(card.getStatus())
				.limit(card.getCardLimit())
				.build();
	}
	
}
