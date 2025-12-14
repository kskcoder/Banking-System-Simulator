package com.tejas.authservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.authservice.models.ChangePasswordRequest;
import com.tejas.authservice.models.LoginRequest;
import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.services.AuthService;
import com.tejas.bankingcommon.dto.ContactDetails;
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
		return ResponseEntity.ok().body(service.signupUser(req));
	}
	
	@PostMapping("/loginuser")
	public ResponseEntity<String> loginUser(@Valid @RequestBody LoginRequest req) {
		return ResponseEntity.ok().body(service.verifyUser(req));
	}
	
	@GetMapping("/getcontact/{userId}")
	public ResponseEntity<ContactDetails> getContact(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.getContact(userId));
	}
	
	@PutMapping("/changepassword/{userId}")
	public ResponseEntity<String> changePassword(@Valid @PathVariable Long userId, @Valid @RequestBody ChangePasswordRequest request) {
		return ResponseEntity.ok().body(service.changePassword(userId, request));
	}	
	
	@PutMapping("/makeadmin/{userId}")
	public ResponseEntity<String> makeAdmin(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.makeAdmin(userId));
	}
	
	@DeleteMapping("/deleteuser/{userId}")
	public ResponseEntity<String> deleteUser(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.deleteUser(userId));
	}	
	
	@PostMapping("/sendotp/{userId}")
	public ResponseEntity<Boolean> sendOtp(@Valid @RequestBody OtpRequestDTO request) {
		return ResponseEntity.ok().body(service.sendOtp(request));
	}
	
	@PostMapping("/submitotp")
	public ResponseEntity<OtpValidateResponse> validateOtp(@Valid @RequestBody OtpValidateRequest request) {
		return ResponseEntity.ok().body(service.validateOtp(request));
	}
}
