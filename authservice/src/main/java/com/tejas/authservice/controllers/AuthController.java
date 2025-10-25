package com.tejas.authservice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.services.AuthService;

@Controller("/auth")
public class AuthController {
	@Autowired
	private AuthService service;
	
	@PostMapping("/signup")
	public ResponseEntity<User> saveUser(@RequestBody SignupRequest req) {
		return service.signupUser(req);
	}
	
	@PostMapping("/login")
	public ResponseEntity<User> loginUser(@RequestParam String username, @RequestParam String password) {
		return service.loginUser(username, password);
	}
}
