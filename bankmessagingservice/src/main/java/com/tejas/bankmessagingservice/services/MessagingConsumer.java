package com.tejas.bankmessagingservice.services;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.MessageEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingConsumer {

	private final EmailService emailService;
	private static final int MAX_CREDIT_RETRY_ATTEMPTS = 3;
	private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
	private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    
	@KafkaListener(topics = "messaging-otp-topic", groupId = "banking-system-simulator-group")
	public void sendOtp(MessageEvent event) {
		emailService.sendOtpEmail(event.getEmail(), event.getOtpNumber());
	}
	

	

}
