package com.tejas.bankcardservice.services;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tejas.bankcardservice.feign.AccountInterface;
import com.tejas.bankcardservice.model.Card;
import com.tejas.bankcardservice.repositories.CardRepo;
import com.tejas.bankcardservice.utils.CardGenerals;
import com.tejas.bankingcommon.enums.AccountType;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LimitResetService {
	private final CardRepo repo;
	private final CardGenerals generals;
	private final AccountInterface accInterface;
	
	@Scheduled(cron = "0 0 0 * * ?")
	public void resetDailyLimit() {
		List<Card> list = repo.findAll();
		
		if (!list.isEmpty()) {
			for (Card card: list) {
				AccountType type = accInterface.getAccountTypeByAccountId(card.getAccountId()).getBody();
				card.setCardLimit(generals.getDailyLimit(type));
				repo.save(card);
			}
		}
	}
}
