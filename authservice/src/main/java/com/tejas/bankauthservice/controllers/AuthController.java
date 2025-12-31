package com.tejas.bankauthservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankauthservice.models.ChangePasswordRequest;
import com.tejas.bankauthservice.models.LoginRequest;
import com.tejas.bankauthservice.models.SignupRequest;
import com.tejas.bankauthservice.models.User;
import com.tejas.bankauthservice.services.AuthService;
import com.tejas.bankingcommon.dto.ContactDetails;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService service;
	
	@PostMapping("/signup")
	public ResponseEntity<User> saveUser(@Valid @RequestBody SignupRequest req) {
		return ResponseEntity.ok().body(service.signupUser(req));
	}
	
	@PostMapping("/loginuser")
	public ResponseEntity<String> loginUser(@Valid @RequestBody LoginRequest req) {
		return ResponseEntity.ok().body(service.verifyUser(req));
	}
	
	@PutMapping("/changepassword/{userId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<String> changePassword(@Valid @PathVariable Long userId, @Valid @RequestBody ChangePasswordRequest request) {
		return ResponseEntity.ok().body(service.changePassword(userId, request));
	}	
	
	@PutMapping("/makeadmin/{userId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<String> makeAdmin(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.makeAdmin(userId));
	}
	
	@DeleteMapping("/deleteuser/{userId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<String> deleteUser(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.deleteUser(userId));
	}	
	
	//INTERNAL METHODS
	@GetMapping("/getcontact/{userId}")
	public ResponseEntity<ContactDetails> getContact(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.getContact(userId));
	}
	
	@PostMapping("/sendotp/{userId}")
	public ResponseEntity<Boolean> sendPaymentOtp(@Valid @RequestBody OtpRequestDTO request) {
		return ResponseEntity.ok().body(service.sendOtp(request));
	}
	
	@PostMapping("/submitotp")
	public ResponseEntity<OtpValidateResponse> validateOtp(@Valid @RequestBody OtpValidateRequest request) {
		return ResponseEntity.ok().body(service.validateOtp(request));
	}
}
