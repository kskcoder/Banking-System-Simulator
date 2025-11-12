package com.tejas.transactionservice.services;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;

@Service
public class TransactionConsumer {

	private static final int MAX_CREDIT_RETRY_ATTEMPTS = 3;
	private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
	private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

	@Autowired
	TransactionProducer trProducer;

	@Autowired
	TransactionService trService;

	@KafkaListener(topics = "transaction-debit-topic", groupId = "banking-system-simulator-group")
	public void debitReceiver(TransactionEvent trEvent) {
		if (TransactionStatus.DEBIT_SUCCESS.toString().equals(trEvent.getStatus())) {
			trService.saveTransaction(trEvent);
			dispatchCreditWithRetry(trEvent);
		} else {
			trService.saveTransaction(trEvent);
		}
	}
	
	@KafkaListener(topics = "transaction-debit-repaid-topic", groupId = "banking-system-simulator-group")
	public void debitRepaidReceiver(TransactionEvent trEvent) {
		trService.saveTransaction(trEvent);
	}

	@KafkaListener(topics = "transaction-credit-topic", groupId = "banking-system-simulator-group")
	public void creditReceiver(TransactionEvent trEvent) {
		if (TransactionStatus.CREDIT_SUCCESS.toString().equals(trEvent.getStatus())) {
			trEvent.setStatus(TransactionStatus.SUCCESS.toString());
			trService.saveTransaction(trEvent);
		} else {
			trEvent.setStatus(TransactionStatus.CREDIT_FAILED.toString());
			trService.saveTransaction(trEvent);
			dispatchCreditWithRetry(swapSenderReceiverForRepay(trEvent));
		}
	}

	private void dispatchCreditWithRetry(TransactionEvent trEvent) {
		attemptCreditDispatch(trEvent, 1, INITIAL_BACKOFF);
	}

	private void attemptCreditDispatch(TransactionEvent trEvent, int attempt, Duration backoff) {
		trProducer.creditRequest(trEvent).whenComplete((result, ex) -> {
			if (ex != null) {
				if (attempt >= MAX_CREDIT_RETRY_ATTEMPTS) {
					trEvent.setStatus(TransactionStatus.CREDIT_FAILED.toString());
					trService.saveTransaction(trEvent);						
					dispatchCreditWithRetry(swapSenderReceiverForRepay(trEvent));
				} else {
					trService.saveTransaction(trEvent);
					Duration nextBackoff = backoff.multipliedBy(2);
					RETRY_EXECUTOR.schedule(() -> attemptCreditDispatch(trEvent, attempt + 1, nextBackoff),
							backoff.toMillis(), TimeUnit.MILLISECONDS);
				}
			}
		});					
	}
	
	private TransactionEvent swapSenderReceiverForRepay(TransactionEvent trEvent) {
		TransactionEvent trEventTemp = trEvent;
		String tempSender = trEventTemp.getFromAccountNumber();
		trEventTemp.setFromAccountNumber(trEventTemp.getToAccountNumber());
		trEventTemp.setToAccountNumber(tempSender);
		trEventTemp.setStatus(TransactionStatus.REPAY_PENDING.toString());
		return trEventTemp;
	}
	

}
