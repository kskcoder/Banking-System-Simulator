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

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/card")
public class CardController {
	private final CardService cardService;
	
	@GetMapping("/admin/all")
	@Operation(summary = "Get all cards", description = "Retrieve all cards (Admin only)")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<GeneralCardResponse>> getAllCards() {
		return ResponseEntity.ok().body(cardService.getAllCards());
	}
	
	@GetMapping("/user/all")
	@Operation(summary = "Get user cards", description = "Retrieve all cards for the authenticated user")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<GeneralCardResponse>> getAllUserCards() {
		return ResponseEntity.ok().body(cardService.getUsersAllCards());
	}
	
	@PostMapping("/create/{accountId}")
	@Operation(summary = "Create card", description = "Create a new card for an account")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to create card for",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<FirstCardResponse> createCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.createCard(accountId));
	}
	
	@GetMapping("/account/{accountId}")
	@Operation(summary = "Get card by account ID", description = "Retrieve card details by account ID")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<GeneralCardResponse> getCardByAccountId(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.getCardByAccountId(accountId));
	}
	
	@PutMapping("/block/{accountId}")
	@Operation(summary = "Block card", description = "Block a card by account ID")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to block card for",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<GeneralCardResponse> blockCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.blockCard(accountId));
	}
	
	@PutMapping("/unblock/{accountId}")
	@Operation(summary = "Unblock card", description = "Unblock a card by account ID")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to unblock card for",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<GeneralCardResponse> unblockCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.unblockCard(accountId));
	}
	
	@DeleteMapping("/delete/{accountId}")
	@Operation(summary = "Delete card", description = "Delete a card by account ID")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to delete card for",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<GeneralCardResponse> deleteCard(@PathVariable long accountId) {
		return ResponseEntity.ok().body(cardService.deleteCard(accountId));
	}
	
	//INTERNAL METHODS
	@Hidden
	@PostMapping("/verify")
	public ResponseEntity<CardVerificationResponse> verifyCard(@Valid @RequestBody CardVerificationRequest request) {
		return ResponseEntity.ok().body(cardService.verifyCard(request));
	}
}
