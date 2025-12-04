package com.tejas.authservice.services;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tejas.authservice.OtpUtils;
import com.tejas.authservice.exceptions.AlreadyUsedException;
import com.tejas.authservice.models.LoginRequest;
import com.tejas.authservice.models.Otp;
import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.repositories.AuthRepo;
import com.tejas.bankingcommon.dto.OtpEvent;
import com.tejas.bankingcommon.dto.OtpReferenceId;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpType;
import com.tejas.bankingcommon.enums.UserType;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankingcommon.exceptions.UnauthorizedException;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthRepo repo;
	private final AuthenticationManager authManager;
	private final JWTService jwtService;
	private final AuthUserDetailsService userDetailsService;
	private final AuthOtpProducer otpProducer;
	
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

	public ResponseEntity<User> signupUser(SignupRequest req) {
		User user = new User();
		
		user = repo.getByUsername(req.getUsername()).orElse(null);
		if (user != null) {
			throw new AlreadyUsedException("Username: "+req.getUsername());
		} 
		
		user = repo.getByEmail(req.getEmail()).orElse(null);
		if (user != null) {
			throw new AlreadyUsedException("Email: "+req.getEmail());
		}
		
		user = repo.getByEmail(req.getEmail()).orElse(null);
		user.setUsername(req.getUsername());
		user.setEmail(req.getEmail());
		user.setPassword(encoder.encode(req.getPassword()));
		user.setPhone(req.getPhone());
		user.setRole(UserType.USER);
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

	public ResponseEntity<Boolean> sendPaymentOtp(OtpRequestDTO request) {
		User user = repo.getByUserId(request.getUserId()).orElse(null);
		
		if (user == null) {throw new NotFoundException("User not found");}
		
		String rawOtp = OtpUtils.otpGenerator();
		
		Otp otp = Otp.builder()
				.otpHash(OtpUtils.hash(rawOtp))
				.type(OtpType.PAYMENT)
				.referenceId(String.valueOf(request.getReferenceId()))
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.attempts(0)
				.maxAttempts(3)
				.used(false)
				.build();
		
		OtpEvent event = OtpEvent.builder()
				.otpNumber(rawOtp)
				.email(user.getEmail())
				.type(OtpType.PAYMENT)
				.build();
		
		otpProducer.dispatchResponseWithRetry(event);
		
		
		return ResponseEntity.ok().body(true);
	}
}
