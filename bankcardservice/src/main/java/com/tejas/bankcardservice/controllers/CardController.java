package com.tejas.bankcardservice.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankcardservice.model.Card;
import com.tejas.bankcardservice.services.CardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/card")
public class CardController {
	private final CardService cardService;
	
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Card>> getAllCards() {
		return cardService.getAllCards();
	}
	
	@PostMapping("/create/{accountId}")
	public ResponseEntity<Card> createCard(@PathVariable long accountId) {
		return cardService.createCard(accountId);
	}
	
	@GetMapping("/account/{accountId}")
	public ResponseEntity<Card> getCardByAccountId(@PathVariable long accountId) {
		return cardService.getCardByAccountId(accountId);
	}
	
	@PutMapping("/block/{cardNumber}")
	public ResponseEntity<Card> blockCard(@PathVariable long cardNumber) {
		return cardService.blockCard(cardNumber);
	}
	
	@PutMapping("/unblock/{cardNumber}")
	public ResponseEntity<Card> unblockCard(@PathVariable long cardNumber) {
		return cardService.unblockCard(cardNumber);
	}
}
