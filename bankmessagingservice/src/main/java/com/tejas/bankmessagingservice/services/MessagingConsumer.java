package com.tejas.bankmessagingservice.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.MessageEvent;
import com.tejas.bankingcommon.dto.MessageType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingConsumer {

	private final EmailService emailService;
    
	@KafkaListener(topics = "messaging-send-topic", groupId = "banking-system-simulator-group")
	public void sendMessage(MessageEvent event) {
		if (event.getType().equals(MessageType.CREDIT) || event.getType().equals(MessageType.DEBIT)) {
			emailService.sendTransactionEmail(event.getEmail(), event.getType(), event.getMessage());
		} else {
			emailService.sendOtpEmail(event.getEmail(), event.getOtpNumber());
		}		
	}
}
