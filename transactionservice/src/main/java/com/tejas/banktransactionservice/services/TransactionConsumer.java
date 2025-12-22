package com.tejas.banktransactionservice.services;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.bankingcommon.enums.TransactionType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionConsumer {

	private static final int MAX_CREDIT_RETRY_ATTEMPTS = 3;
	private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
	private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final TransactionProducer trProducer;
    private final TransactionAndLedgerUpdater trUpdater;
    
	@KafkaListener(topics = "transaction-debit-topic", groupId = "banking-system-simulator-group")
	public void debitReceiver(TransactionEvent trEvent) {
		trUpdater.saveTransaction(trEvent);
		trUpdater.saveTransactionRecord(trEvent, false);
		
		if (TransactionStatus.DEBIT_SUCCESS.equals(trEvent.getStatus())) {
			TransactionEvent credEvent = createCreditEvent(trEvent);
			
			dispatchCreditWithRetry(credEvent);
		} else if (trEvent.getPaymentId() != null) {
			trProducer.dispatchMessageWithRetry(trEvent);
		}
	}
	
	@KafkaListener(topics = "transaction-debit-repaid-topic", groupId = "banking-system-simulator-group")
	public void debitRepaidReceiver(TransactionEvent trEvent) {
		trUpdater.saveTransaction(trEvent);
		
		if (TransactionStatus.REPAY_FAILED.equals(trEvent.getStatus())) {
			dispatchCreditWithRetry(trEvent);
		} else if (TransactionStatus.REPAY_SUCCESS.equals(trEvent.getStatus())) {
			trUpdater.saveTransactionRecord(trEvent, true);
		}
	}

	@KafkaListener(topics = "transaction-credit-topic", groupId = "banking-system-simulator-group")
	public void creditReceiver(TransactionEvent trEvent) {
		if (trEvent.getTransactionId() == null) {
			trUpdater.saveInterestTransaction(trEvent);
			return;
		}
		trUpdater.saveTransaction(trEvent);
		if (TransactionStatus.CREDIT_SUCCESS.equals(trEvent.getStatus())) {
			trProducer.dispatchMessageWithRetry(trEvent);
			trUpdater.saveTransactionRecord(trEvent, true);
		} else {
			trProducer.dispatchMessageWithRetry(trEvent);
			TransactionEvent repayEvent = createRepayEvent(trEvent);
			dispatchCreditWithRetry(repayEvent);
		}
	}
	
	@KafkaListener(topics = "transaction-interest-topic", groupId = "banking-system-simulator-group")
	public void interestReceiver(TransactionEvent trEvent) {
		trUpdater.saveInterestTransaction(trEvent);
	}

	private void dispatchCreditWithRetry(TransactionEvent trEvent) {
		attemptCreditDispatch(trEvent, 1, INITIAL_BACKOFF);
	}

	private void attemptCreditDispatch(TransactionEvent trEvent, int attempt, Duration backoff) {
		trProducer.creditRequest(trEvent).whenComplete((result, ex) -> {
			if (ex != null) {
				if (attempt >= MAX_CREDIT_RETRY_ATTEMPTS) {
					trEvent.setStatus(TransactionStatus.CREDIT_FAILED);
					trUpdater.saveTransaction(trEvent);						
					TransactionEvent repayEvent = createRepayEvent(trEvent);
					dispatchCreditWithRetry(repayEvent);
				} else {
					trUpdater.saveTransaction(trEvent);
					Duration nextBackoff = backoff.multipliedBy(2);
					RETRY_EXECUTOR.schedule(() -> attemptCreditDispatch(trEvent, attempt + 1, nextBackoff),
							nextBackoff.toMillis(), TimeUnit.MILLISECONDS);
				}
			}
		});					
	}
	
    private TransactionEvent cloneEvent(TransactionEvent e) {
        if (e == null) return null;
        TransactionEvent copy = new TransactionEvent();
        copy.setTransactionId(e.getTransactionId());
        copy.setFromAccountNumber(e.getFromAccountNumber());
        copy.setToAccountNumber(e.getToAccountNumber());
        copy.setAmount(e.getAmount());
        copy.setUserId(e.getUserId());
        copy.setType(e.getType());
        copy.setStatus(e.getStatus());
        return copy;
    }

    private TransactionEvent createCreditEvent(TransactionEvent debitSuccessEvent) {
        TransactionEvent credit = cloneEvent(debitSuccessEvent);
        credit.setType(TransactionType.CREDIT);
        credit.setStatus(TransactionStatus.PENDING);
        return credit;
    }

    private TransactionEvent createRepayEvent(TransactionEvent failedCreditEvent) {
        TransactionEvent repay = cloneEvent(failedCreditEvent);
        repay.setFromAccountNumber(failedCreditEvent.getToAccountNumber());
        repay.setToAccountNumber(failedCreditEvent.getFromAccountNumber());
        repay.setType(failedCreditEvent.getType());
        repay.setStatus(TransactionStatus.REPAY_PENDING);
        return repay;
    }	

}
