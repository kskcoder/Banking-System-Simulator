package com.tejas.authservice.services;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.MessageEvent;

@Service
public class AuthOtpProducer {
	private static final int MAX_CREDIT_RETRY_ATTEMPTS = 3;
	private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
	private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	
	@Autowired
	KafkaTemplate<String, MessageEvent> kafkaTemplate;
	
	public CompletableFuture<SendResult<String, MessageEvent>> otpRequest(MessageEvent event) {
    	return kafkaTemplate.send("messaging-otp-topic", event);
    }

	public void dispatchResponseWithRetry(MessageEvent event) {

		attemptResponseDispatch(event,1, INITIAL_BACKOFF);
	}

	private void attemptResponseDispatch(MessageEvent event, int attempt, Duration backoff) {
		otpRequest(event).whenComplete((result, ex) -> {
				if (ex != null) {
					if (attempt >= MAX_CREDIT_RETRY_ATTEMPTS) {
						return;
					} else {
						Duration nextBackoff = backoff.multipliedBy(2);
						RETRY_EXECUTOR.schedule(() -> attemptResponseDispatch(event, attempt + 1, nextBackoff),
								nextBackoff.toMillis(), TimeUnit.MILLISECONDS);
					}
				}
			});					
	}
}
