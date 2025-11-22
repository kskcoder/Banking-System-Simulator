package com.tejas.accountservice.services;

import java.util.Collections;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tejas.accountservice.enums.AccountType;
import com.tejas.accountservice.models.Account;
import com.tejas.accountservice.repositories.AccountRepo;
import com.tejas.accountservice.utils.AccountRules;
import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.bankingcommon.enums.TransactionType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterestService {
	private final AccountRepo repo;
	private final AccountRules rules;
	private final AccountProducer acProducer;
	
	@Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Kolkata")
	public void processMonthlyInterest() {
		List<Account> list = repo.getByAccountType(AccountType.SAVINGS).orElseGet(Collections::emptyList);
		
		if (list.isEmpty()) {return;}
		
        
		for (Account acc: list) {
			double balance = acc.getBalance();
			double interestAmt = balance * (rules.getInterest(acc.getAccountType(), balance >= 100000) / 1200) ;
			double newBalance = interestAmt + balance;
			
			TransactionEvent trEvent = new TransactionEvent();
	        trEvent.setTransactionId(null);
	        trEvent.setFromAccountNumber("BANK");
	        trEvent.setUserId(null);			
			trEvent.setToAccountNumber(acc.getAccountnumber());
			trEvent.setBalanceAfter(newBalance);
	        trEvent.setAmount(interestAmt);
	        trEvent.setType(TransactionType.INTEREST);
	        trEvent.setStatus(TransactionStatus.CREDIT_SUCCESS.toString()); 
	        
	        acc.setBalance(newBalance);
	        repo.save(acc);
	        acProducer.dispatchResponseWithRetry(trEvent);
	        
		}
		
	}
}
