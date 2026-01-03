package com.tejas.bankpaymentservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankingcommon.dto.OtpValidateResponse;
import com.tejas.bankingcommon.dto.SubmitPaymentOtp;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.services.PaymentService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {
	@Autowired
	PaymentService paymentService;
	
	@GetMapping("/all")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<Payment>> getAllPayments() {
		return ResponseEntity.ok().body(paymentService.getAllPayments());
	}
	
	@PostMapping("/initiate")
	@Parameter(
		name = "X-External",
		in = ParameterIn.HEADER,
		required = true,
		description = "External request identifier. Must be present for payment initiation.",
		example = "1"
	)
	@Parameter(
		name = "X-Vendor-Id",
		in = ParameterIn.HEADER,
		required = true,
		description = "Vendor ID for external requests",
		example = "vendor123"
	)
	@Parameter(
		name = "X-Vendor-Secret",
		in = ParameterIn.HEADER,
		required = true,
		description = "Vendor secret for external requests",
		example = "secret123"
	)
	@Parameter(
		name = "X-Timestamp",
		in = ParameterIn.HEADER,
		required = true,
		description = "Request timestamp",
		example = "1234567890"
	)
	@Parameter(
		name = "X-Signature",
		in = ParameterIn.HEADER,
		required = true,
		description = "Request signature for validation",
		example = "signature123"
	)
	public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentDTO initiateReq) {
		return paymentService.initiateRequest(initiateReq);
	}
	
	@PostMapping("/submitotp")
	@Parameter(
		name = "X-External",
		in = ParameterIn.HEADER,
		required = true,
		description = "External request identifier. Must be present for OTP submission.",
		example = "1"
	)
	@Parameter(
		name = "X-Vendor-Id",
		in = ParameterIn.HEADER,
		required = true,
		description = "Vendor ID for external requests",
		example = "vendor123"
	)
	@Parameter(
		name = "X-Vendor-Secret",
		in = ParameterIn.HEADER,
		required = true,
		description = "Vendor secret for external requests",
		example = "secret123"
	)
	@Parameter(
		name = "X-Timestamp",
		in = ParameterIn.HEADER,
		required = true,
		description = "Request timestamp",
		example = "1234567890"
	)
	@Parameter(
		name = "X-Signature",
		in = ParameterIn.HEADER,
		required = true,
		description = "Request signature for validation",
		example = "signature123"
	)
	public ResponseEntity<OtpValidateResponse> submitOtp(@Valid @RequestBody SubmitPaymentOtp otpRequest) {
		return paymentService.submitOtp(otpRequest);
	}
	
	
}
