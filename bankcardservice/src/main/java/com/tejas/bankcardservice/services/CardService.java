package com.tejas.bankcardservice.services;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankcardservice.exceptions.AlreadyExistsException;
import com.tejas.bankcardservice.feign.AccountInterface;
import com.tejas.bankcardservice.model.Card;
import com.tejas.bankcardservice.repositories.CardRepo;
import com.tejas.bankcardservice.utils.CardGenerator;
import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;

import feign.FeignException;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CardService {
	private final CardRepo repo;
	private final CardGenerator generator;	
	private final AccountInterface accInterface;
	
	public ResponseEntity<Card> createCard(long accountId) {
		if (!isOwner(accountId)) {throw new ForbiddenException("You do not have permission to use this card.");}
		if (repo.existsByAccountId(accountId)) {throw new AlreadyExistsException(String.valueOf(accountId));}
		String rawCardNumber;
		String hashedCardNumber;
		
		do {
			rawCardNumber = generator.cardNumberGenerator();
			hashedCardNumber = generator.hash(rawCardNumber);
			
		} while (repo.existsByCardNumber(hashedCardNumber));
		
		AccountType type = accInterface.getAccountTypeByAccountId(accountId).getBody();
		String rawCvvNumber = generator.cardNumberGenerator();
		String hashedCvvNumber = generator.hash(rawCvvNumber);
		
		String expiryDate = generator.expiryGenerator();
		
		Card card = Card.builder()
				.cardNumber(hashedCardNumber)
				.cvv(hashedCvvNumber)
				.expiryDate(expiryDate)
				.cardLimit(type == AccountType.SAVINGS ? 50000.0 : 200000.0)
				.isBlocked(false)
				.accountId(accountId)
				.build();
		
		repo.save(card);
		
		return ResponseEntity.ok().body(card);
	}

	public ResponseEntity<Card> getCardByAccountId(long accountId) {
		if (!isOwner(accountId)) {throw new ForbiddenException("You do not have permission to use this card.");}
		Card card = repo.getByAccountId(accountId).get();
		
		if (card != null) {
			return ResponseEntity.ok().body(card);
		}
			
		throw new ForbiddenException("You do not have permission to access this resource.");
	}
	
	public ResponseEntity<Card> blockCard(long cardNumber) {
		String hashedCardNumber = generator.hash(String.valueOf(cardNumber)); 
		Card card = repo.getByCardNumber(hashedCardNumber).get();
		if (card != null) {
			if (!isOwner(card.getAccountId())) {throw new ForbiddenException("You do not have permission to use this card.");}
			
			card.setBlocked(true);
			repo.save(card);
			return ResponseEntity.ok().body(card);
		}
		
		throw new GeneralServerException();		
	}
	
	
	public ResponseEntity<Card> unblockCard(long cardNumber) {
		String hashedCardNumber = generator.hash(String.valueOf(cardNumber)); 
		Card card = repo.getByCardNumber(hashedCardNumber).get();
		if (card != null) {
			if (!isOwner(card.getAccountId())) {throw new ForbiddenException("You do not have permission to use this card.");}
			
			card.setBlocked(false);
			repo.save(card);
			return ResponseEntity.ok().body(card);
		}
		
		throw new GeneralServerException();
	}

	private boolean isOwner(long pathAccId) {
		String role = ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
		try {
			 boolean isOwner = accInterface.isOwnerOfAccountId(pathAccId).getBody();
			 if (!isOwner && !role.equals("ADMIN")) {
				 return false;     				
	    	}
	    } catch (FeignException e) {
	    	return false;
	    }
		return true;
	}
	
	//Admin related functions

	public ResponseEntity<List<Card>> getAllCards() {
		List<Card> cards = repo.findAll();
		
		if (!cards.isEmpty()) {
			return ResponseEntity.ok().body(cards);
		}
		
		throw new NoContentException("No cards found.");
	}
}
