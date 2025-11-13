package com.tejas.accountservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tejas.accountservice.models.Account;
import com.tejas.accountservice.repositories.AccountRepo;
import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;

public class AccountConsumer {
	@Autowired
	AccountRepo repo;
	
	@Autowired
	AccountProducer accProducer;
	
	@KafkaListener(topics = "account-debit-topic", groupId = "banking-system-simulator-group")
	public void debitSender(TransactionEvent trEvent) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		String senderAccNumber = trEvent.getFromAccountNumber();
		Double amount = trEvent.getAmount();
		
		Account account = repo.getByAccountnumber(senderAccNumber).orElse(null);
		
		if (account == null) {
			trEvent.setStatus(TransactionStatus.DEBIT_FAILED.toString());			
		} else if (String.valueOf(account.getUserid()).equals(userId)) {
			if (amount > account.getBalance()) {
				trEvent.setStatus(TransactionStatus.INSUFFICIENT_BALANCE.toString());
			} else {
				account.setBalance(account.getBalance() - amount);
				trEvent.setStatus(TransactionStatus.DEBIT_SUCCESS.toString());
				repo.save(account);
			}
		} else {
			trEvent.setStatus(TransactionStatus.UNAUTHORISED.toString());
		}
		
		accProducer.dispatchDebitResponseWithRetry(trEvent);
	}
	
	@KafkaListener(topics = "account-credit-topic", groupId = "banking-system-simulator-group")
	public void creditReceiver(TransactionEvent trEvent) {
		String status = trEvent.getStatus();
		Boolean repay = TransactionStatus.REPAY_PENDING.toString().equals(status);
		String senderAccNumber = trEvent.getToAccountNumber();
		Double amount = trEvent.getAmount();
		
		Account account = repo.getByAccountnumber(senderAccNumber)
				.orElse(null);
		
		if (account == null) {
			trEvent.setStatus(repay ? TransactionStatus.REPAY_FAILED.toString() : TransactionStatus.CREDIT_FAILED.toString());
		} else {
			account.setBalance(account.getBalance() + amount);
			repo.save(account);
			trEvent.setStatus(repay ? TransactionStatus.REPAY_SUCCESS.toString() : TransactionStatus.CREDIT_SUCCESS.toString());
		}
		
		accProducer.dispatchDebitResponseWithRetry(trEvent);
	}
}
