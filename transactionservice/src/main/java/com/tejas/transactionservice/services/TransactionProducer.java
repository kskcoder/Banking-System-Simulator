package com.tejas.transactionservice.services;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionProducer {

	private static final int MAX_CREDIT_RETRY_ATTEMPTS = 3;
	private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
	private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

	private final StatusUpdater trUpdater;	
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

	public CompletableFuture<SendResult<String, TransactionEvent>> debitRequest(TransactionEvent event) {
    	return kafkaTemplate.send("account-debit-topic", event);
    }
	
	public CompletableFuture<SendResult<String, TransactionEvent>> creditRequest(TransactionEvent event) {
    	return kafkaTemplate.send("account-credit-topic", event);
    }
	
	public void dispatchDebitWithRetry(TransactionEvent trEvent) {
		attemptDebitDispatch(trEvent, 1, INITIAL_BACKOFF);
	}

	private void attemptDebitDispatch(TransactionEvent trEvent, int attempt, Duration backoff) {
			debitRequest(trEvent).whenComplete((result, ex) -> {
				if (ex != null) {
					if (attempt >= MAX_CREDIT_RETRY_ATTEMPTS) {
						trEvent.setStatus(TransactionStatus.DEBIT_FAILED.toString());
						trUpdater.saveTransaction(trEvent);
					} else {
						trEvent.setStatus(TransactionStatus.RETRY.toString());
						trUpdater.saveTransaction(trEvent);
						Duration nextBackoff = backoff.multipliedBy(2);
						RETRY_EXECUTOR.schedule(() -> attemptDebitDispatch(trEvent, attempt + 1, nextBackoff),
								nextBackoff.toMillis(), TimeUnit.MILLISECONDS);
					}
				}
			});					
	}
	
	
}
