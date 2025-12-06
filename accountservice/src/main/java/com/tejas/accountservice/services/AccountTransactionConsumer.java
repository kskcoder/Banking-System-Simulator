package com.tejas.accountservice.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.tejas.accountservice.feign.AuthInterface;
import com.tejas.accountservice.models.Account;
import com.tejas.accountservice.repositories.AccountRepo;
import com.tejas.accountservice.utils.AccountRules;
import com.tejas.bankingcommon.dto.ContactDetails;
import com.tejas.bankingcommon.dto.MessageEvent;
import com.tejas.bankingcommon.dto.MessageType;
import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountTransactionConsumer {	
	private final AccountRepo repo;	
	private final AccountTransactionProducer accProducer;	
	private final AccountMessageProducer acMsgProducer;
	private final AccountRules rules;
	private final AuthInterface authInt;
	
	@KafkaListener(topics = "account-debit-topic", groupId = "banking-system-simulator-group")
	public void debitSender(TransactionEvent trEvent) {
		String userId = String.valueOf(trEvent.getUserId());
		String senderAccNumber = trEvent.getFromAccountNumber();
		Double amount = trEvent.getAmount();
		
		Account account = repo.getByAccountnumber(senderAccNumber).orElse(null);
		
		if (account == null) {
			trEvent.setStatus(TransactionStatus.DEBIT_FAILED.toString());			
		} else if (String.valueOf(account.getUserid()).equals(userId)) {
			double newBalance = account.getBalance() - amount;
			if (newBalance < rules.getMinimumBalance(account.getAccountType())) {
				trEvent.setStatus(TransactionStatus.INSUFFICIENT_BALANCE.toString());
			} else {
				account.setBalance(newBalance);
				trEvent.setBalanceAfter(newBalance);
				trEvent.setStatus(TransactionStatus.DEBIT_SUCCESS.toString());
				
				ContactDetails details = authInt.getContact(account.getUserid()).getBody();
				
				String message = account.getId() + "," + amount + "," +newBalance;
						
				MessageEvent event = MessageEvent.builder()
						.email(details.getEmail())
						.type(MessageType.DEBIT)
						.message(message)
						.build();
				
				acMsgProducer.dispatchResponseWithRetry(event);
				repo.save(account);
			}
		} else {
			trEvent.setStatus(TransactionStatus.UNAUTHORISED.toString());
		}
		
		accProducer.dispatchResponseWithRetry(trEvent);
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
			double newBalance = account.getBalance() + amount;
			account.setBalance(newBalance);
			trEvent.setBalanceAfter(newBalance);
			
			ContactDetails details = authInt.getContact(account.getUserid()).getBody();
			
			String message = account.getId() + "," + amount + "," +newBalance;
					
			MessageEvent event = MessageEvent.builder()
					.email(details.getEmail())
					.type(MessageType.CREDIT)
					.message(message)
					.build();
			
			acMsgProducer.dispatchResponseWithRetry(event);
			
			repo.save(account);
			trEvent.setStatus(repay ? TransactionStatus.REPAY_SUCCESS.toString() : TransactionStatus.CREDIT_SUCCESS.toString());
		}
		
		accProducer.dispatchResponseWithRetry(trEvent);
	}
}
