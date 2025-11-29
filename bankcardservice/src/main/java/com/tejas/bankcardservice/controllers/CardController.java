package com.tejas.bankcardservice.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankcardservice.dtos.FirstCardResponse;
import com.tejas.bankcardservice.dtos.GeneralCardResponse;
import com.tejas.bankcardservice.model.Card;
import com.tejas.bankcardservice.services.CardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/card")
public class CardController {
	private final CardService cardService;
	
	@GetMapping("/admin/all")
	public ResponseEntity<List<Card>> getAllCards() {
		return cardService.getAllCards();
	}
	
	@GetMapping("/user/all")
	public ResponseEntity<List<GeneralCardResponse>> getAllUserCards() {
		return cardService.getUsersAllCards();
	}
	
	@PostMapping("/create/{accountId}")
	public ResponseEntity<FirstCardResponse> createCard(@PathVariable long accountId) {
		return cardService.createCard(accountId);
	}
	
	@GetMapping("/account/{accountId}")
	public ResponseEntity<GeneralCardResponse> getCardByAccountId(@PathVariable long accountId) {
		return cardService.getCardByAccountId(accountId);
	}
	
	@PutMapping("/block/{accountId}")
	public ResponseEntity<Card> blockCard(@PathVariable long accountId) {
		return cardService.blockCard(accountId);
	}
	
	@PutMapping("/unblock/{accountId}")
	public ResponseEntity<Card> unblockCard(@PathVariable long accountId) {
		return cardService.unblockCard(accountId);
	}
	
	@DeleteMapping("/delete/{accountId}")
	public ResponseEntity<Card> deleteCard(@PathVariable long accountId) {
		return cardService.deleteCard(accountId);
	}
}
