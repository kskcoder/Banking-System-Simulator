package com.tejas.accountservice.services;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.tejas.bankingcommon.dto.TransactionEvent;

public class AccountProducer {
	private static final int MAX_CREDIT_RETRY_ATTEMPTS = 3;
	private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
	private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	
	@Autowired
	KafkaTemplate<String, TransactionEvent> kafkaTemplate;
	
	public CompletableFuture<SendResult<String, TransactionEvent>> debitResponse(TransactionEvent event) {
    	return kafkaTemplate.send("transaction-debit-topic", event);
    }
	
	public CompletableFuture<SendResult<String, TransactionEvent>> debitRepayResponse(TransactionEvent event) {    	
    	return kafkaTemplate.send("transaction-debit-repaid-topic", event);
    }
	
	public CompletableFuture<SendResult<String, TransactionEvent>> creditResponse(TransactionEvent event) {    	
    	return kafkaTemplate.send("transaction-credit-topic", event);
    }
	
	public void dispatchDebitResponseWithRetry(TransactionEvent trEvent) {
		String status = trEvent.getStatus();
		CompletableFuture<SendResult<String, TransactionEvent>> function = status.contains("CREDIT") 
				? creditResponse(trEvent) 
					: status.contains("REPAY") 
						? debitRepayResponse(trEvent)
							: debitResponse(trEvent);
		
		attemptDebitResponseDispatch(function, 1, INITIAL_BACKOFF);
	}

	private void attemptDebitResponseDispatch(CompletableFuture<SendResult<String, TransactionEvent>> function, int attempt, Duration backoff) {
		function.whenComplete((result, ex) -> {
				if (ex != null) {
					if (attempt >= MAX_CREDIT_RETRY_ATTEMPTS) {
						return;
					} else {
						Duration nextBackoff = backoff.multipliedBy(2);
						RETRY_EXECUTOR.schedule(() -> attemptDebitResponseDispatch(function, attempt + 1, nextBackoff),
								backoff.toMillis(), TimeUnit.MILLISECONDS);
					}
				}
			});					
	}
}
