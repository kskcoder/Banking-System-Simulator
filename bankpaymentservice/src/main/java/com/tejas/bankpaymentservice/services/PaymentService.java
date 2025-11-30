package com.tejas.bankpaymentservice.services;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankingcommon.dto.CardVerificationRequest;
import com.tejas.bankingcommon.dto.CardVerificationResponse;
import com.tejas.bankingcommon.dto.UserContactDetails;
import com.tejas.bankingcommon.enums.PaymentStatus;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankpaymentservice.feign.AccountInterface;
import com.tejas.bankpaymentservice.feign.CardInterface;
import com.tejas.bankpaymentservice.feign.UserInterface;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.repositories.PaymentRepo;

import feign.FeignException;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PaymentService {
	private final PaymentRepo repo;
	private final CardInterface cardInt;
	private final AccountInterface accInt;
	private final UserInterface userInt;
	
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
			repo.save(payment);
		} catch(Exception e) {
			throw new GeneralServerException();
		}
		
		return processInitialPayment(initiateReq, payment);
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
				
				return ResponseEntity.ok().body(paymentRes);
			}
		}		
		return ResponseEntity.ok().body(null);
	}
	
	public ResponseEntity<PaymentResponse> postCardValidation(InitiatePaymentDTO initiateReq, Payment payment, CardVerificationResponse cardVerifyResponse) {
		if (cardVerifyResponse.isValidated()) {
			try {
				long userId = accInt.getuserIdByAccountId(cardVerifyResponse.getAccountId()).getBody();
				UserContactDetails userDetails = userInt.getUserDetailsByUserId(userId).getBody();
				
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
				
				return ResponseEntity.ok().body(paymentRes);
			}
			
		}
		return ResponseEntity.ok().body(null);
	}

}
