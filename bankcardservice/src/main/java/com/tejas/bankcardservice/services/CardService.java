package com.tejas.bankcardservice.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankcardservice.dtos.FirstCardResponse;
import com.tejas.bankcardservice.dtos.GeneralCardResponse;
import com.tejas.bankcardservice.exceptions.AlreadyExistsException;
import com.tejas.bankcardservice.feign.AccountInterface;
import com.tejas.bankcardservice.model.Card;
import com.tejas.bankcardservice.repositories.CardRepo;
import com.tejas.bankcardservice.utils.CardGenerals;
import com.tejas.bankcardservice.utils.CardMask;
import com.tejas.bankcardservice.utils.GeneralUtils;
import com.tejas.bankingcommon.dto.CardVerificationRequest;
import com.tejas.bankingcommon.dto.CardVerificationResponse;
import com.tejas.bankingcommon.enums.AccountCardStatus;
import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankingcommon.enums.UserType;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;

import feign.FeignException;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CardService {
	private final CardRepo repo;
	private final CardGenerals generals;	
	private final AccountInterface accInterface;
	
	public ResponseEntity<FirstCardResponse> createCard(long accountId) {
		if (!isOwner(accountId)) {throw new ForbiddenException("You do not have permission to use this card.");}
		if (repo.existsByAccountId(accountId)) {throw new AlreadyExistsException(String.valueOf(accountId));}
		String rawCardNumber;
		String hashedCardNumber;
		
		do {
			rawCardNumber = generals.cardNumberGenerator();
			hashedCardNumber = generals.hash(rawCardNumber);
			
		} while (repo.existsByCardNumber(hashedCardNumber));
		
		AccountType type = accInterface.getAccountTypeByAccountId(accountId).getBody();
		String rawCvvNumber = generals.cvvGenerator();
		String hashedCvvNumber = generals.hash(rawCvvNumber);
		
		String expiryDate = generals.expiryGenerator();
		
		Card card = Card.builder()
				.cardNumber(hashedCardNumber)
				.cvv(hashedCvvNumber)
				.lastDigits(rawCardNumber.substring(11,16))
				.expiryDate(expiryDate)
				.cardLimit(generals.getDailyLimit(AccountType.SAVINGS))
				.status(AccountCardStatus.INACTIVE)
				.accountId(accountId)
				.build();
		
		repo.save(card);
		
		FirstCardResponse cardNew = FirstCardResponse.builder()
				.cardNumber(CardMask.formatCardNumber(rawCardNumber))
				.cvv(rawCvvNumber)
				.expiry(expiryDate)
				.maskedCardNumber(CardMask.maskCardNumber(rawCardNumber, false))
				.status(AccountCardStatus.INACTIVE)
				.limit(type == AccountType.SAVINGS ? 50000.0 : 200000.0)
				.build();
		
		return ResponseEntity.ok().body(cardNew);
	}

	public ResponseEntity<GeneralCardResponse> getCardByAccountId(long accountId) {
		if (!isOwner(accountId)) {throw new ForbiddenException("You do not have permission to use this card.");}
		Card card = repo.getByAccountId(accountId).orElse(null);
		
		if (card != null) {
			String lastDigits = card.getLastDigits();
			GeneralCardResponse cardNew = GeneralCardResponse.builder()
					.expiry(card.getLastDigits())
					.maskedCardNumber(CardMask.maskCardNumber(lastDigits, true))
					.status(card.getStatus())
					.limit(card.getCardLimit())
					.build();
			return ResponseEntity.ok().body(cardNew);
		} 
		
		throw new NotFoundException("No card associated with Account ID: "+accountId);
	}
	
	public ResponseEntity<List<GeneralCardResponse>> getUsersAllCards() {
		int userId = Integer.parseInt(GeneralUtils.getUserId());
		
		List<Long> accountIds = accInterface.getAccountIdsByUserId().getBody();
		
		if (!accountIds.isEmpty()) {
			List<Card> cards = new ArrayList<>();
			
			for (Long id: accountIds) {
				cards.add(repo.getByAccountId(id).orElse(null));
			}
			
			if (!cards.isEmpty()) {
				List<GeneralCardResponse> responseCards = new ArrayList<>();
				for (Card c: cards) {
					String lastDigits = c.getLastDigits();
					GeneralCardResponse cardNew = GeneralCardResponse.builder()
							.expiry(c.getLastDigits())
							.maskedCardNumber(CardMask.maskCardNumber(lastDigits, true))
							.status(c.getStatus())
							.limit(c.getCardLimit())
							.build();
					
					responseCards.add(cardNew);
				}
				return ResponseEntity.ok().body(responseCards);
			} else {
				throw new NotFoundException("No card associated with User ID: "+userId);
			}
			
		} else {
			throw new NotFoundException("No accounts associated with User ID: "+userId);
		}
	}
	
	public ResponseEntity<Card> blockCard(long accountId) {
		Card card = repo.getByAccountId(accountId).orElse(null);
		if (card != null) {
			if (!isOwner(card.getAccountId())) {throw new ForbiddenException("You do not have permission to use this card.");}
			
			card.setStatus(AccountCardStatus.BLOCKED);
			repo.save(card);
			return ResponseEntity.ok().body(card);
		}
		
		throw new NotFoundException("No card associated with Account ID: "+accountId);		
	}
	
	
	public ResponseEntity<Card> unblockCard(long accountId) {
		Card card = repo.getByAccountId(accountId).orElse(null);
		if (card != null) {
			if (!isOwner(card.getAccountId())) {throw new ForbiddenException("You do not have permission to use this card.");}
			
			card.setStatus(AccountCardStatus.ACTIVE);
			repo.save(card);
			return ResponseEntity.ok().body(card);
		}
		
		throw new NotFoundException("No card associated with Account ID: "+accountId);
	}
	
	public ResponseEntity<Card> deleteCard(long accountId) {
		Card card = repo.getByAccountId(accountId).orElse(null);
		if (card != null) {
			if (!isOwner(card.getAccountId())) {throw new ForbiddenException("You do not have permission to use this card.");}
			
			card.setStatus(AccountCardStatus.ACTIVE);
			repo.delete(card);
			return ResponseEntity.ok().body(card);
		}
		
		throw new NotFoundException("No card associated with Account ID: "+accountId);
	}
	
	public ResponseEntity<CardVerificationResponse> verifyCard(@RequestBody CardVerificationRequest request) {
		String hashedCard = generals.hash(request.getCardNumber());
		Card card = repo.getByCardNumber(hashedCard).orElse(null);
		
		if (card != null) {
			String hashedCvv = generals.hash(request.getCvv());
			boolean isCvvCorrect = card.getCvv().equals(hashedCvv);
			boolean matchesExpiry = card.getExpiryDate().equals(request.getExpiryDate());
			
			if (!matchesExpiry) {
				throw new BadRequestException("Incorrect expiry date");
			}
			
			boolean isExpired = generals.isExpired(request.getExpiryDate());
			
			boolean hasExceededLimit = (card.getCardLimit() - request.getAmount()) < 0;
			
			if (isCvvCorrect && !isExpired) {
				throw new BadRequestException("Card has expired.");
			} else if (!isCvvCorrect && isExpired) {
				throw new BadRequestException("Incorrect CVV number.");
			} else if (!isCvvCorrect && !isExpired) {
				throw new BadRequestException("Incorrect CVV number and Card has expired.");
			} else if (hasExceededLimit) {
				throw new BadRequestException("Card has exceeded daily limit.");
			} else if (card.getStatus() == AccountCardStatus.BLOCKED) {
				throw new BadRequestException("Card is blocked.");
			} else {
				CardVerificationResponse reponse = CardVerificationResponse.builder()
						.accountId(card.getAccountId())
						.validated(true)
						.build();
				return ResponseEntity.ok().body(reponse);
			}
			
		} else {
			throw new NotFoundException("Card not found");
		}
	}
	

	private boolean isOwner(long pathAccId) {
		String role = ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
		
		if (role.equals(UserType.INTERNAL_SERVICE.toString()) || role.equals(UserType.ADMIN.toString())) {
			return true;
		}
		
		try {
			 boolean isOwner = accInterface.isOwnerOfAccountId(pathAccId).getBody();
			 if (isOwner) {
				 return true;     				
	    	 } else {
	    		 return false;
	    	 }
	    } catch (FeignException e) {
	    	return false;
	    }
	}
	
	//Admin related functions

	public ResponseEntity<List<Card>> getAllCards() {
		String role = ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
		
		if (!role.equals("ADMIN")) {throw new ForbiddenException("You do not have permission to access this resource.");}
		
		List<Card> cards = repo.findAll();
		
		if (!cards.isEmpty()) {
			return ResponseEntity.ok().body(cards);
		}
		
		throw new NoContentException("No cards found.");
	}

}
