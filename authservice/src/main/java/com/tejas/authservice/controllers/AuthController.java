package com.tejas.authservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.authservice.models.LoginRequest;
import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.services.AuthService;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService service;
	
	@PostMapping("/signup")
	public ResponseEntity<User> saveUser(@Valid @RequestBody SignupRequest req) {
		return service.signupUser(req);
	}
	
	@PostMapping("/loginuser")
	public ResponseEntity<String> loginUser(@Valid @RequestBody LoginRequest req) {
		return service.verifyUser(req);
	}
	
	@PostMapping("/sendotp/{userId}")
	public ResponseEntity<Boolean> sendOtp(@Valid @RequestBody OtpRequestDTO request) {
		return service.sendOtp(request);
	}
	
	@PostMapping("/submitotp")
	public ResponseEntity<OtpValidateResponse> validateOtp(@Valid @RequestBody OtpValidateRequest request) {
		return service.validateOtp(request);
	}
}
