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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tejas.bankingcommon.dto.CardVerificationRequest;
import com.tejas.bankingcommon.dto.CardVerificationResponse;
import com.tejas.bankingcommon.dto.MessageType;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;
import com.tejas.bankingcommon.dto.SubmitPaymentOtp;
import com.tejas.bankingcommon.dto.TransferRequest;
import com.tejas.bankingcommon.enums.PaymentStatus;
import com.tejas.bankingcommon.enums.PaymentType;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankpaymentservice.feign.AccountInterface;
import com.tejas.bankpaymentservice.feign.AuthInterface;
import com.tejas.bankpaymentservice.feign.CardInterface;
import com.tejas.bankpaymentservice.feign.TransactionInterface;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.repositories.PaymentRepo;
import com.tejas.bankpaymentservice.utils.HmacUtil;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	
	@Autowired
	private ObjectMapper objectMapper;
	
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
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes();
		if (attributes == null) {
			throw new GeneralServerException();
		}
		String vendorId = attributes.getRequest().getHeader("X-Vendor-Id");
		
		Payment payment = Payment.builder()
				.vendorId(vendorId)
				.fromAccountNumber(null)
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
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes();
		if (attributes == null) {
			throw new ForbiddenException("You do not have permission to access this resource.");
		}
		String role = attributes.getRequest().getHeader("X-User-Role");
		
		if (role == null || !role.equals("ADMIN")) {
			throw new ForbiddenException("You do not have permission to access this resource.");
		}
		
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
		if (initiateReq.getType() == PaymentType.CARD) {
			CardVerificationRequest verifyReq = CardVerificationRequest.builder()
					.cardNumber(initiateReq.getCardNumber())			
					.cvv(initiateReq.getCvv())
					.expiryDate(initiateReq.getExpiry())
					.build();
			
		try {
			CardVerificationResponse cardVerifyResponse = cardInt.verifyCard(verifyReq).getBody();
			if (cardVerifyResponse == null) {
				throw new GeneralServerException();
			}
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
		} else if (initiateReq.getType() == PaymentType.UPI) {
			return ResponseEntity.ok().body(null);
		}
		return ResponseEntity.ok().body(null);
	}
	
	public ResponseEntity<PaymentResponse> postCardValidation(InitiatePaymentDTO initiateReq, Payment payment, CardVerificationResponse cardVerifyResponse) {
		if (cardVerifyResponse != null && cardVerifyResponse.isValidated()) {
			try {
				String accountNumber = accInt.getAccountNumberByAccountId(cardVerifyResponse.getAccountId()).getBody();
				if (accountNumber == null) {
					throw new GeneralServerException();
				}
				payment.setFromAccountNumber(accountNumber);
				repo.save(payment);
				evictPaymentCache(payment.getId());
				
				Long userIdLong = accInt.getuserIdByAccountId(cardVerifyResponse.getAccountId()).getBody();
				if (userIdLong == null) {
					throw new GeneralServerException();
				}
				long userId = userIdLong;
				OtpRequestDTO otpReq = OtpRequestDTO.builder()
						.referenceId(payment.getId())
						.type(MessageType.PAYMENT_OTP)
						.userId(userId)
						.build();
				
				try {
					authInt.sendPaymentOtp(userId, otpReq).getBody();
					
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
		System.out.println("Payment Service - submitOtp called for paymentId: " + otpRequest.getPaymentId() + ", OTP: " + otpRequest.getOtp());
		
		Payment payment = getCachedPayment(otpRequest.getPaymentId());
		
		if (payment != null) {
			System.out.println("Payment Service - Payment found, status: " + payment.getStatus());
			
			if (payment.getStatus().equals(PaymentStatus.FAILED)) {
				System.out.println("Payment Service - Payment has failed status, returning FORBIDDEN");
				OtpValidateResponse submitResponse = OtpValidateResponse.builder()
						.referenceId(String.valueOf(otpRequest.getPaymentId()))
						.validated(false)
						.message("Payment has failed, initiate new payment")
						.build();
				
				return new ResponseEntity<>(submitResponse, HttpStatus.FORBIDDEN);
			}
			
			OtpValidateRequest submitRequest = OtpValidateRequest.builder()
					.referenceId(String.valueOf(otpRequest.getPaymentId()))
					.type(MessageType.PAYMENT_OTP)
					.otpValue(otpRequest.getOtp())
					.build();
			
			System.out.println("Payment Service - Calling auth service to validate OTP for paymentId: " + otpRequest.getPaymentId());
			
			OtpValidateResponse otpResponse = new OtpValidateResponse(); 
			boolean isValidated = false;
			
			try {
				otpResponse = authInt.validateOtp(submitRequest).getBody();
				if (otpResponse == null) {
					throw new GeneralServerException();
				}
				isValidated = otpResponse.isValidated();
				System.out.println("Payment Service - OTP validation result from auth service: " + isValidated);
			} catch (FeignException e) {
				System.out.println("Payment Service - Error validating OTP: status=" + e.status() + ", message=" + e.contentUTF8());

				payment.setStatus(PaymentStatus.INCORRECT_OTP);
				repo.save(payment);
				evictPaymentCache(payment.getId());
				evictAllPaymentsCache();
				
				OtpValidateResponse errorResponse = OtpValidateResponse.builder()
						.referenceId(String.valueOf(otpRequest.getPaymentId()))
						.validated(false)
						.message("OTP validation failed")
						.build();
				return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
			}
			
			if (isValidated) {
				if (otpResponse.isRetried()) {
					return new ResponseEntity<>(otpResponse, HttpStatus.BAD_REQUEST);
				}
				System.out.println("Payment Service - OTP is valid, proceeding with payment processing");
				return validOtp(payment);
			} else {
				payment.setStatus(PaymentStatus.INCORRECT_OTP);
				repo.save(payment);
				evictPaymentCache(payment.getId());
				evictAllPaymentsCache();
				
				return new ResponseEntity<>(otpResponse, HttpStatus.UNAUTHORIZED);
			}
			
		} else {
			System.out.println("Payment Service - Payment not found for paymentId: " + otpRequest.getPaymentId());
			throw new NotFoundException("Incorrect Payment Id");
		}
	}
	
	public ResponseEntity<OtpValidateResponse> validOtp(Payment payment) {
		System.out.println("Payment Service - validOtp called for paymentId: " + payment.getId());
		
		TransferRequest req = TransferRequest.builder()
				.fromAccount(payment.getFromAccountNumber())
				.toAccount(payment.getToAccountNumber())
				.amount(payment.getAmount())
				.paymentId(payment.getId())
				.build();
		
		System.out.println("Payment Service - Calling transaction service to transfer: from=" + req.getFromAccount() + ", to=" + req.getToAccount() + ", amount=" + req.getAmount());
		
		try {
			trInt.transfer(req).getBody();
			System.out.println("Payment Service - Transfer completed successfully");
			
			//Not needed to check if transaction call has succeeded as failure is caught as exception. 
		} catch (FeignException e) {
			System.out.println("Payment Service - Error calling transaction service: status=" + e.status() + ", message=" + e.contentUTF8());
			e.printStackTrace();
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
		return repo.findById(paymentId)
			.orElseThrow(() -> new NotFoundException("Incorrect Payment Id"));
	}
	
	public String calculateSignatureDemo(String requestBody, String vendorSecret, String timestamp) {
		try {
			String bodyString = normalizeJsonBody(requestBody);
			return HmacUtil.hmacSha256(vendorSecret, bodyString + timestamp);
		} catch (Exception e) {
			log.error("Payment Service - Error calculating signature: {}", e.getMessage(), e);
			throw new GeneralServerException();
		}
	}
	
	private String normalizeJsonBody(String body) {
		try {
			com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(body);
			String normalized = objectMapper.writeValueAsString(jsonNode);
			
			normalized = normalized.replaceAll("\\s*:\\s*", ":");
			normalized = normalized.replaceAll("\\s*,\\s*", ",");
			normalized = normalized.replaceAll("\\s*\\{\\s*", "{");
			normalized = normalized.replaceAll("\\s*\\}\\s*", "}");
			normalized = normalized.replaceAll("\\s*\\[\\s*", "[");
			normalized = normalized.replaceAll("\\s*\\]\\s*", "]");
			
			normalized = normalized.replaceAll("\"amount\":(\\d+),", "\"amount\":$1.0,");
			normalized = normalized.replaceAll("\"amount\":(\\d+)\\}", "\"amount\":$1.0}");
			
			return normalized;
		} catch (Exception e) {
			log.warn("Payment Service - Failed to normalize JSON body, using original: {}", e.getMessage());
			return body;
		}
	}
}