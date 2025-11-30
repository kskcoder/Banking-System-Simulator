package com.tejas.bankpaymentservice.services;

import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.Card;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankingcommon.dto.CardVerificationDTO;
import com.tejas.bankingcommon.enums.AccountCardStatus;
import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankingcommon.enums.PaymentStatus;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankpaymentservice.feign.CardInterface;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.repositories.PaymentRepo;

import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PaymentService {
	private final PaymentRepo repo;
	private final CardInterface cardInt;
	
	public ResponseEntity<PaymentResponse> initiateRequest(@Valid InitiatePaymentDTO initiateReq) {
		
		Payment payment = Payment.builder()
				.vendorId(initiateReq.getVendorId())
				.fromAccountNumber(initiateReq.getFromAccountNumber())
				.toAccountNumber(initiateReq.getToAccountNumber())
				.amount(initiateReq.getAmount())
				.type(initiateReq.getType())
				.status(PaymentStatus.INITIATED)
				.build();
		try {
			repo.save(payment);
		} catch(Exception e) {
			throw new GeneralServerException();
		}
		
		if (initiateReq.getType() == 1) {
			CardVerificationDTO verifyReq = CardVerificationDTO.builder()
					.cardNumber(initiateReq.getCardNumber())			
					.cvv(String.valueOf(initiateReq.getCvv()))
					.expiryDate(initiateReq.getExpiry())
					.build();
			
			try {
				boolean cardVerified = cardInt.verifyCard(verifyReq).getBody();
				
				if (cardVerified) {
					
				}
			} catch (FeignException e) {
				throw new BadRequestException(e.contentUTF8());
			}
			
		}
		
		PaymentResponse paymentRes = PaymentResponse.builder()
				.paymentId(payment.getId())
				.status(PaymentStatus.INITIATED)
				.build();
		
		
		
		return ResponseEntity.ok().body(paymentRes);
	}
	
	public ResponseEntity<FirstCardResponse> createCard(long accountId) {
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
				.lastDigits(rawCardNumber.substring(11,16))
				.expiryDate(expiryDate)
				.cardLimit(type == AccountType.SAVINGS ? 50000.0 : 200000.0)
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

	public ResponseEntity<List<Payment>> getAllPayments() {
		String role = ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
		
		if (!role.equals("ADMIN")) {throw new ForbiddenException("You do not have permission to access this resource.");}
		
		List<Payment> payments = repo.findAll();
		
		if (!payments.isEmpty()) {
			return ResponseEntity.ok().body(payments);
		}
		
		throw new NoContentException("No payments found.");
	}

}
