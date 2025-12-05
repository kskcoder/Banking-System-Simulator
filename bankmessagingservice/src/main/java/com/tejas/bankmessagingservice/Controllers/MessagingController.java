package com.tejas.bankmessagingservice.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankingcommon.dto.MessageEvent;
import com.tejas.bankmessagingservice.services.MessagingConsumer;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/message")
public class MessagingController {
	private final MessagingConsumer msgConsumer;
	
//	@PostMapping("/testOtp")
	public ResponseEntity<Boolean> testOtp(@Valid @RequestBody MessageEvent event) {
		msgConsumer.sendOtp(event);
		return ResponseEntity.ok().body(true);
	}
}
