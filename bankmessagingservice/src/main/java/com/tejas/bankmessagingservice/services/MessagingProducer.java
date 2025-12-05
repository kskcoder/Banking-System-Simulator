package com.tejas.bankmessagingservice.services;

import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingProducer {

//	private static final int MAX_CREDIT_RETRY_ATTEMPTS = 3;
//	private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
//	private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

	
//	public void dispatchDebitWithRetry(TransactionEvent trEvent) {
//		attemptDebitDispatch(trEvent, 1, INITIAL_BACKOFF);
//	}
//
//	private void attemptDebitDispatch(TransactionEvent trEvent, int attempt, Duration backoff) {
//			debitRequest(trEvent).whenComplete((result, ex) -> {
//				if (ex != null) {
//					if (attempt >= MAX_CREDIT_RETRY_ATTEMPTS) {
//						trEvent.setStatus(TransactionStatus.DEBIT_FAILED.toString());
//						trUpdater.saveTransaction(trEvent);
//					} else {
//						trEvent.setStatus(TransactionStatus.RETRY.toString());
//						trUpdater.saveTransaction(trEvent);
//						Duration nextBackoff = backoff.multipliedBy(2);
//						RETRY_EXECUTOR.schedule(() -> attemptDebitDispatch(trEvent, attempt + 1, nextBackoff),
//								nextBackoff.toMillis(), TimeUnit.MILLISECONDS);
//					}
//				}
//			});					
//	}
	
}
