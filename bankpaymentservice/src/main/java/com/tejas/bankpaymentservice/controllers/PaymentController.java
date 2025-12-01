package com.tejas.bankpaymentservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankingcommon.dto.SubmitOtp;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.services.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {
	@Autowired
	PaymentService paymentService;
	
	@GetMapping
	public ResponseEntity<List<Payment>> getAllPayments() {
		return paymentService.getAllPayments();
	}
	
	@PostMapping("/initiate")
	public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentDTO initiateReq) {
		return paymentService.initiateRequest(initiateReq);
	}
	
	@PostMapping("/submitotp")
	public ResponseEntity<PaymentResponse> submitOtp(@Valid @RequestBody SubmitOtp otpRequest) {
		return paymentService.submitOtp(otpRequest);
	}
	
	
}
