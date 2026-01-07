package com.tejas.bankpaymentservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankingcommon.dto.OtpValidateResponse;
import com.tejas.bankingcommon.dto.SubmitPaymentOtp;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.models.SignatureDemoResponse;
import com.tejas.bankpaymentservice.services.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	@Operation(
		summary = "Initiate payment",
		description = "Initiate a payment request. For testing/demo purposes, you can use POST /payments/demo/calculate-signature to calculate the required signature. " +
				"Send the same request body and headers (X-Vendor-Secret, X-Timestamp) to the demo endpoint to get the signature value. " +
				"Then use the returned signature, timestamp, and body values in this endpoint. " +
				"Note: Use vendorId='TPay' in request body and vendorSecret='gatewayTPayVendorKey' in X-Vendor-Secret header. Only these values are allowed for now."
	)
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
		description = "Vendor ID for external requests. Default: TPay. Note: Only 'TPay' is allowed for now.",
		example = "TPay"
	)
	@Parameter(
		name = "X-Vendor-Secret",
		in = ParameterIn.HEADER,
		required = true,
		description = "Vendor secret for external requests. Default: gatewayTPayVendorKey. Note: Only 'gatewayTPayVendorKey' is allowed for now. Tip: Use POST /payments/demo/calculate-signature with the same body and this secret to get the signature.",
		example = "gatewayTPayVendorKey"
	)
	@Parameter(
		name = "X-Timestamp",
		in = ParameterIn.HEADER,
		required = true,
		description = "Request timestamp. Tip: Use the same timestamp value returned from POST /payments/demo/calculate-signature.",
		example = "1234567890"
	)
	@Parameter(
		name = "X-Signature",
		in = ParameterIn.HEADER,
		required = true,
		description = "Request signature for validation. Tip: Calculate using POST /payments/demo/calculate-signature endpoint or use the signature value from its response.",
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
	
	@PostMapping("/demo/calculate-signature")
	@Operation(
		summary = "Calculate signature (Demo only)", 
		description = "Helper endpoint to calculate signature for testing purposes. This endpoint is for demo/testing only and should NOT be used in production. External vendors must calculate signatures themselves using their secret keys. WARNING: This endpoint returns vendorSecret in the response for demo convenience - this should NEVER be done in production. " +
				"Note: Use vendorId='TPay' and vendorSecret='gatewayTPayVendorKey' in request body and headers respectively. Only these values are allowed for now."
	)
	@Tag(name = "Demo", description = "Demo endpoints for testing")
	public ResponseEntity<SignatureDemoResponse> calculateSignature(
			@Valid @RequestBody InitiatePaymentDTO requestBody,
			@RequestHeader("X-Vendor-Secret") @Parameter(name = "X-Vendor-Secret", in = ParameterIn.HEADER, required = true, description = "Vendor secret key. Default: gatewayTPayVendorKey. Note: Only 'gatewayTPayVendorKey' is allowed for now.", example = "gatewayTPayVendorKey") String vendorSecret,
			@RequestHeader("X-Timestamp") @Parameter(name = "X-Timestamp", in = ParameterIn.HEADER, required = true, description = "Request timestamp") String timestamp) {
		
		return ResponseEntity.ok(paymentService.calculateSignatureDemo(requestBody, vendorSecret, timestamp));
	}
	
}
