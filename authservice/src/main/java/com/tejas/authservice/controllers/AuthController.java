package com.tejas.authservice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.authservice.models.LoginRequest;
import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.services.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
	@Autowired
	private AuthService service;
	
	@PostMapping("/signup")
	public ResponseEntity<User> saveUser(@RequestBody SignupRequest req) {
		return service.signupUser(req);
	}
	
	@PostMapping("/loginuser")
	public ResponseEntity<String> loginUser(@RequestBody LoginRequest req) {
		return service.verifyUser(req);
	}
}
