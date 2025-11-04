package com.tejas.authservice.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tejas.authservice.models.LoginRequest;
import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.repositories.AuthRepo;

@Service
public class AuthService {
	@Autowired
	AuthRepo repo;
	
	@Autowired
	AuthenticationManager authManager;
	
	@Autowired
	JWTService jwtService;
	
	@Autowired
	AuthUserDetailsService userDetailsService;
	
	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

	public ResponseEntity<User> signupUser(SignupRequest req) {
		User user = new User();
		user.setUsername(req.getUsername());
		user.setEmail(req.getEmail());
		user.setPassword(encoder.encode(req.getPassword()));
		user.setPhone(req.getPhone());
		user.setRole("USER");
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		repo.save(user);
		
		return new ResponseEntity<>(user, HttpStatus.OK);		
	}

	public ResponseEntity<String> verifyUser(LoginRequest req) {
		String username = req.getUsername();
		String password = req.getPassword();
		
		try {
			Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
			
			if (authentication.isAuthenticated()) {
				User user = repo.getByUsername(username);
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				return new ResponseEntity<>(jwtService.generateToken(user.getId(), userDetails), HttpStatus.OK);
			}
		} catch (Exception e) {
			return new ResponseEntity<>("Failure", HttpStatus.UNAUTHORIZED);
		}
		
		return new ResponseEntity<>("Failure", HttpStatus.UNAUTHORIZED);
	}
}
