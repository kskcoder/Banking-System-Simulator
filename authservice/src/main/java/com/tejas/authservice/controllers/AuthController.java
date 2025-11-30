package com.tejas.authservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.authservice.models.LoginRequest;
import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.services.AuthService;
import com.tejas.bankingcommon.dto.UserContactDetails;

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
	
	@GetMapping("/{userId}/contact")
	public ResponseEntity<UserContactDetails> getUserDetailsByUserId(@PathVariable long userId) {
		return service.getUserDetailsByUserId(userId);
	}
}
