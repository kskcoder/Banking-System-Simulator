package com.tejas.accountservice.services;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionType;

@Service
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
	
	public CompletableFuture<SendResult<String, TransactionEvent>> interestCreditResponse(TransactionEvent event) {    	
    	return kafkaTemplate.send("transaction-interest-topic", event);
    }
	
	public void dispatchResponseWithRetry(TransactionEvent trEvent) {
		CompletableFuture<SendResult<String, TransactionEvent>> function;
		TransactionType type = trEvent.getType();
		String status = trEvent.getStatus();

		if (TransactionType.INTEREST.equals(type)) {
			function = interestCreditResponse(trEvent);
		} else if (status != null && status.contains(TransactionType.REPAY.toString())) {
			function = debitRepayResponse(trEvent);
		} else if (TransactionType.CREDIT.equals(type) || (status != null && status.contains(TransactionType.CREDIT.toString()))) {
			function = creditResponse(trEvent);
		} else {
			function = debitResponse(trEvent);
		}

		attemptResponseDispatch(function, 1, INITIAL_BACKOFF);
	}

	private void attemptResponseDispatch(CompletableFuture<SendResult<String, TransactionEvent>> function, int attempt, Duration backoff) {
		function.whenComplete((result, ex) -> {
				if (ex != null) {
					if (attempt >= MAX_CREDIT_RETRY_ATTEMPTS) {
						return;
					} else {
						Duration nextBackoff = backoff.multipliedBy(2);
						RETRY_EXECUTOR.schedule(() -> attemptResponseDispatch(function, attempt + 1, nextBackoff),
								nextBackoff.toMillis(), TimeUnit.MILLISECONDS);
					}
				}
			});					
	}
}
