package com.tejas.authservice.services;

import java.time.LocalDateTime;

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
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankingcommon.exceptions.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthRepo repo;
	private final AuthenticationManager authManager;
	private final JWTService jwtService;
	private final AuthUserDetailsService userDetailsService;
	
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

	public ResponseEntity<User> signupUser(SignupRequest req) {
		User user = new User();
		user.setUsername(req.getUsername());
		user.setEmail(req.getEmail());
		user.setPassword(encoder.encode(req.getPassword()));
		user.setPhone(req.getPhone());
		user.setRole("USER");
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		try {
			repo.save(user);
		} catch (Exception e) {
			throw new GeneralServerException();
		}
		
		return ResponseEntity.ok().body(user);		
	}

	public ResponseEntity<String> verifyUser(LoginRequest req) {
		String username = req.getUsername();
		String password = req.getPassword();
		
		User user = repo.getByUsername(username).orElse(null);
		
		if (user == null) {throw new NotFoundException("User not found");}
		
		try {
			Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
			
			if (authentication.isAuthenticated()) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				return ResponseEntity.ok().body(jwtService.generateToken(user.getId(), userDetails));
			} 
		} catch (Exception e) {
			throw new UnauthorizedException("Incorrect Password");
		}
		
		throw new GeneralServerException();
	}
}
