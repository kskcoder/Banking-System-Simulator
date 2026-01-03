package com.tejas.bankpaymentservice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.tejas.bankingcommon.dto.*;
import com.tejas.bankingcommon.enums.PaymentStatus;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankpaymentservice.feign.*;
import com.tejas.bankpaymentservice.models.*;
import com.tejas.bankpaymentservice.repositories.PaymentRepo;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {
	private final PaymentRepo repo;
	private final CardInterface cardInt;
	private final AccountInterface accInt;
	private final AuthInterface authInt;
	private final TransactionInterface trInt;
	
	@Autowired
	private CacheManager cacheManager;
	
	public void evictPaymentCache(Long paymentId) {
		Cache paymentCache = cacheManager.getCache("payment_details");
		if (paymentCache != null && paymentId != null) {
			paymentCache.evict(paymentId);
		}
	}
	
	private void evictAllPaymentsCache() {
		Cache allPaymentsCache = cacheManager.getCache("all_payments");
		if (allPaymentsCache != null) {
			allPaymentsCache.clear();
		}
	}
	
	public ResponseEntity<PaymentResponse> initiateRequest(InitiatePaymentDTO initiateReq) {
		
		Payment payment = Payment.builder()
				.vendorId(initiateReq.getVendorId())
				.fromAccountNumber(initiateReq.getFromAccountNumber())
				.toAccountNumber(initiateReq.getToAccountNumber())
				.amount(initiateReq.getAmount())
				.type(initiateReq.getType())
				.status(PaymentStatus.INITIATED)
				.build();
		try {
			Payment savedPayment = repo.save(payment);
			evictAllPaymentsCache();
			return processInitialPayment(initiateReq, savedPayment);
		} catch(Exception e) {
			throw new GeneralServerException();
		}
	}
	
	//Admin related functions
	public List<Payment> getAllPayments() {
		String role = ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
		
		if (!role.equals("ADMIN")) {throw new ForbiddenException("You do not have permission to access this resource.");}
		
		List<Payment> payments = cachedGetAllPayments();
		
		if (!payments.isEmpty()) {
			return payments;
		}
		
		throw new NoContentException("No payments found.");
	}
	
	@Cacheable(value="all_payments", unless="#result == null || #result.isEmpty()")
	protected List<Payment> cachedGetAllPayments() {
		return repo.findAll();
	}
	
	//Helper functions
	public ResponseEntity<PaymentResponse> processInitialPayment(InitiatePaymentDTO initiateReq, Payment payment) {
		if (initiateReq.getType() == 1) {
			CardVerificationRequest verifyReq = CardVerificationRequest.builder()
					.cardNumber(initiateReq.getCardNumber())			
					.cvv(String.valueOf(initiateReq.getCvv()))
					.expiryDate(initiateReq.getExpiry())
					.build();
			
			try {
				CardVerificationResponse cardVerifyResponse = cardInt.verifyCard(verifyReq).getBody();
				return postCardValidation(initiateReq, payment, cardVerifyResponse);
				
			} catch (FeignException e) {				
				PaymentResponse paymentRes = PaymentResponse.builder()
						.paymentId(payment.getId())
						.status(PaymentStatus.FAILED)
						.message(e.contentUTF8())
						.build();
				
				payment.setStatus(PaymentStatus.FAILED);
				repo.save(payment);
				evictPaymentCache(payment.getId());
				evictAllPaymentsCache();
				
				return ResponseEntity.ok().body(paymentRes);
			}
		}		
		return ResponseEntity.ok().body(null);
	}
	
	public ResponseEntity<PaymentResponse> postCardValidation(InitiatePaymentDTO initiateReq, Payment payment, CardVerificationResponse cardVerifyResponse) {
		if (cardVerifyResponse.isValidated()) {
			try {
				long userId = accInt.getuserIdByAccountId(cardVerifyResponse.getAccountId()).getBody();
				OtpRequestDTO otpReq = OtpRequestDTO.builder()
						.referenceId(payment.getId())
						.type(MessageType.PAYMENT_OTP)
						.userId(userId)
						.build();
				
				try {
					authInt.sendPaymentOtp(otpReq).getBody();
					
					//Not needed to check if call has succeeded as failure is caught as exception.
				} catch (FeignException e) {
					throw new GeneralServerException();
				}
								
				PaymentResponse paymentRes = PaymentResponse.builder()
						.paymentId(payment.getId())
						.status(PaymentStatus.INITIATED)
						.message("OTP has been sent")
						.build();
				
				return ResponseEntity.ok().body(paymentRes);
			} catch (FeignException e) {				
				PaymentResponse paymentRes = PaymentResponse.builder()
						.paymentId(payment.getId())
						.status(PaymentStatus.FAILED)
						.message(e.contentUTF8())
						.build();
				
				payment.setStatus(PaymentStatus.FAILED);
				repo.save(payment);
				evictPaymentCache(payment.getId());
				evictAllPaymentsCache();
				
				return ResponseEntity.ok().body(paymentRes);
			}
			
		} 
		return ResponseEntity.ok().body(null);
	}

	public ResponseEntity<OtpValidateResponse> submitOtp(SubmitPaymentOtp otpRequest) {
		Payment payment = getCachedPayment(otpRequest.getPaymentId());
		
		if (payment != null) {
			if (payment.getStatus().equals(PaymentStatus.FAILED)) {
				OtpValidateResponse submitResponse = OtpValidateResponse.builder()
						.referenceId(String.valueOf(otpRequest.getPaymentId()))
						.validated(false)
						.message("Payment has failed, inititate new payment")
						.build();
				
				return new ResponseEntity<>(submitResponse, HttpStatus.FORBIDDEN);
			}
			
			OtpValidateRequest submitRequest = OtpValidateRequest.builder()
					.referenceId(String.valueOf(otpRequest.getPaymentId()))
					.type(MessageType.PAYMENT_OTP)
					.otpValue(otpRequest.getOtp())
					.build();
			
			boolean isValidated = false;
			
			try {
				isValidated = authInt.validateOtp(submitRequest).getBody().isValidated();				
			} catch (FeignException e) {
				OtpValidateResponse submitResponse = OtpValidateResponse.builder()
						.referenceId(String.valueOf(otpRequest.getPaymentId()))
						.validated(isValidated)
						.message(e.contentUTF8())
						.build();
				
				payment.setStatus(PaymentStatus.INCORRECT_OTP);
				repo.save(payment);
				evictPaymentCache(payment.getId());
				evictAllPaymentsCache();
				
				return new ResponseEntity<>(submitResponse, HttpStatus.UNAUTHORIZED);
			}
			
			if (isValidated) {			
				return validOtp(payment);
			}
			
		} else {
			throw new NotFoundException("Incorrect Payment Id");
		}
		
		throw new GeneralServerException();
	}
	
	public ResponseEntity<OtpValidateResponse> validOtp(Payment payment) {
		TransferRequest req = TransferRequest.builder()
				.fromAccount(payment.getFromAccountNumber())
				.toAccount(payment.getToAccountNumber())
				.amount(payment.getAmount())
				.paymentId(payment.getId())
				.build();
		
		try {
			trInt.transfer(req).getBody();
			
			//Not needed to check if transaction call has succeeded as failure is caught as exception. 
		} catch (FeignException e) {
			throw new GeneralServerException();
		}
		OtpValidateResponse submitResponse = OtpValidateResponse.builder()
				.referenceId(String.valueOf(payment.getId()))
				.validated(true)
				.message("OTP verified. Payment is processing.")
				.build();
		
		payment.setStatus(PaymentStatus.OTP_VERIFIED);
		repo.save(payment);
		evictPaymentCache(payment.getId());
		evictAllPaymentsCache();
		
		return ResponseEntity.ok().body(submitResponse);
	}
	
	@Cacheable(value="payment_details", key="#paymentId", unless="#result == null")
	protected Payment getCachedPayment(Long paymentId) {
		Payment payment = repo.getById(paymentId);
		if (payment == null) {
			throw new NotFoundException("Incorrect Payment Id");
		}
		return payment;
	}
}