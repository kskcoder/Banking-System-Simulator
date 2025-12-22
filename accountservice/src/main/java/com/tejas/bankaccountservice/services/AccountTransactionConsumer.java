package com.tejas.bankaccountservice.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.tejas.bankaccountservice.feign.AuthInterface;
import com.tejas.bankaccountservice.models.Account;
import com.tejas.bankaccountservice.repositories.AccountRepo;
import com.tejas.bankaccountservice.utils.AccountRules;
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
			trEvent.setStatus(TransactionStatus.DEBIT_FAILED);			
		} else if (String.valueOf(account.getUserid()).equals(userId)) {
			double newBalance = account.getBalance() - amount;
			if (newBalance < rules.getMinimumBalance(account.getAccountType())) {
				trEvent.setStatus(TransactionStatus.INSUFFICIENT_BALANCE);
			} else {
				account.setBalance(newBalance);
				trEvent.setBalanceAfter(newBalance);
				trEvent.setStatus(TransactionStatus.DEBIT_SUCCESS);
				
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
			trEvent.setStatus(TransactionStatus.UNAUTHORISED);
		}
		
		accProducer.dispatchResponseWithRetry(trEvent);
	}
	
	@KafkaListener(topics = "account-credit-topic", groupId = "banking-system-simulator-group")
	public void creditReceiver(TransactionEvent trEvent) {
		TransactionStatus status = trEvent.getStatus();
		Boolean repay = TransactionStatus.REPAY_PENDING.equals(status);
		String senderAccNumber = trEvent.getToAccountNumber();
		Double amount = trEvent.getAmount();
		
		Account account = repo.getByAccountnumber(senderAccNumber)
				.orElse(null);
		
		if (account == null) {
			trEvent.setStatus(repay ? TransactionStatus.REPAY_FAILED : TransactionStatus.CREDIT_FAILED);
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
			trEvent.setStatus(repay ? TransactionStatus.REPAY_SUCCESS : TransactionStatus.CREDIT_SUCCESS);
		}
		
		accProducer.dispatchResponseWithRetry(trEvent);
	}
}
