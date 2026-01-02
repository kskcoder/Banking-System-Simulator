package com.tejas.bankcardservice.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankcardservice.dtos.FirstCardResponse;
import com.tejas.bankcardservice.dtos.GeneralCardResponse;
import com.tejas.bankcardservice.services.CardService;
import com.tejas.bankingcommon.dto.CardVerificationRequest;
import com.tejas.bankingcommon.dto.CardVerificationResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/card")
public class CardController {
	private final CardService cardService;
	
	@GetMapping("/admin/all")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<GeneralCardResponse>> getAllCards() {
		return ResponseEntity.ok().body(cardService.getAllCards());
	}
	
	@GetMapping("/user/all")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<GeneralCardResponse>> getAllUserCards() {
		return ResponseEntity.ok().body(cardService.getUsersAllCards());
	}
	
	@PostMapping("/create/{accountId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<FirstCardResponse> createCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.createCard(accountId));
	}
	
	@GetMapping("/account/{accountId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<GeneralCardResponse> getCardByAccountId(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.getCardByAccountId(accountId));
	}
	
	@PutMapping("/block/{accountId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<GeneralCardResponse> blockCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.blockCard(accountId));
	}
	
	@PutMapping("/unblock/{accountId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<GeneralCardResponse> unblockCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.unblockCard(accountId));
	}
	
	@DeleteMapping("/delete/{accountId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<GeneralCardResponse> deleteCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.deleteCard(accountId));
	}
	
	//INTERNAL METHODS
	@PostMapping("/verify")
	public ResponseEntity<CardVerificationResponse> verifyCard(@RequestBody CardVerificationRequest request) {
		return ResponseEntity.ok().body(cardService.verifyCard(request));
	}
}
