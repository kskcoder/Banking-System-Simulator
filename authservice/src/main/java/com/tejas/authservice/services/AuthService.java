package com.tejas.authservice.services;

import java.time.LocalDateTime;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tejas.authservice.exceptions.AlreadyUsedException;
import com.tejas.authservice.models.ChangePasswordRequest;
import com.tejas.authservice.models.LoginRequest;
import com.tejas.authservice.models.Otp;
import com.tejas.authservice.models.SignupRequest;
import com.tejas.authservice.models.User;
import com.tejas.authservice.repositories.AuthRepo;
import com.tejas.authservice.repositories.OtpRepo;
import com.tejas.authservice.utils.AuthUtils;
import com.tejas.authservice.utils.OtpUtils;
import com.tejas.bankingcommon.dto.ContactDetails;
import com.tejas.bankingcommon.dto.MessageEvent;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpStatus;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;
import com.tejas.bankingcommon.enums.UserType;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankingcommon.exceptions.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthRepo repo;
	private final OtpRepo otpRepo;
	private final AuthenticationManager authManager;
	private final JWTService jwtService;
	private final AuthUserDetailsService userDetailsService;
	private final AuthOtpProducer otpProducer;
	
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

	public User signupUser(SignupRequest req) {
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
		user.setPassword(encoder.encode(AuthUtils.hash(req.getPassword())));
		user.setPhone(req.getPhone());
		user.setRole(UserType.USER);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		try {
			repo.save(user);
		} catch (Exception e) {
			throw new GeneralServerException();
		}
		
		return user;		
	}

	public String verifyUser(LoginRequest req) {
		String username = req.getUsername();
		String password = req.getPassword();
		String hashedPassword = AuthUtils.hash(password);
		
		User user = repo.getByUsername(username).orElse(null);
		
		if (user == null) {throw new NotFoundException("User not found");}
		
		try {
			Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(username, hashedPassword));
			
			if (authentication.isAuthenticated()) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				return jwtService.generateToken(user.getId(), userDetails);
			} else {
				throw new UnauthorizedException("Incorrect Password");
			}
		} catch (Exception e) {
			throw new UnauthorizedException("Incorrect Password");
		}
	}
	
	public String changePassword(long userId, ChangePasswordRequest request) {
		User user = repo.getById(userId)
			.orElseThrow(() -> 
				new NotFoundException("User not found")
			);
		
		if (user == null) {throw new NotFoundException("User not found");}
		String id = AuthUtils.getUserId();
		
		if (String.valueOf(userId).equals(id) || AuthUtils.isAdmin()) {
			String hashedOldPassword = user.getPassword();
			String hashedSentOldPassword = AuthUtils.hash(request.getOldPassword());
			String hashedSentNewPassword = AuthUtils.hash(request.getNewPassword());
			if (hashedOldPassword.equals(hashedSentOldPassword)) {
				if (hashedOldPassword.equals(hashedSentNewPassword)) {
					throw new BadRequestException("Incorrect old password.");
				} else {
					user.setPassword(hashedSentNewPassword);
					repo.save(user);
				}				
			} else {
				throw new BadRequestException("Incorrect old password.");
			}
		} else {
			throw new UnauthorizedException("You do not have access to this account.");
		}
		
		return "User deleted successfully.";
	}	

	public String makeAdmin(long userId) {
		User user = repo.getById(userId)
			.orElseThrow(() -> 
				new NotFoundException("User not found")
			);
		
		if (user == null) {throw new NotFoundException("User not found");}
		
		if (AuthUtils.isAdmin()) {
			if (user.getRole().equals(UserType.ADMIN)) {
				throw new BadRequestException("User is already an admin.");
			} else {
				user.setRole(UserType.ADMIN);
				repo.save(user);
			}
		}
		return "Changed role to admin successfully.";
	}

	public String deleteUser(long userId) {
		User user = repo.getById(userId)
			.orElseThrow(() -> 
				new NotFoundException("User not found")
			);
		
		if (user == null) {throw new NotFoundException("User not found");}
		String id = AuthUtils.getUserId();
		
		if (String.valueOf(userId).equals(id) || AuthUtils.isAdmin()) {
			repo.delete(user);
		} else {
			throw new UnauthorizedException("You do not have access to this account.");
		}
				
		return "User deleted successfully.";
	}
	
	public ContactDetails getContact(Long userId) {
		String id = AuthUtils.getUserId();
		if (id.equals("INTERNAL_PAYMENT_SERVICE") || AuthUtils.isAdmin()) {
			return cachedGetContact(userId);
		} else {
			throw new UnauthorizedException("You do not have access to this account.");
		}
	}
	
	@Cacheable(value="contact_details", key="userId", unless="#result == null")
	protected ContactDetails cachedGetContact(long userId) {
		User user = repo.getById(userId)
			.orElseThrow(() -> 
				new NotFoundException("User not found")
			);
		
		ContactDetails details = ContactDetails.builder()
				.email(user.getEmail())
				.phone(user.getPhone())
				.build();
		
			return details;
	}

	public Boolean sendOtp(OtpRequestDTO request) {
		User user = repo.getById(request.getUserId())
			.orElseThrow(() -> 
				new NotFoundException("User not found")
			);
		
		String rawOtp = OtpUtils.otpGenerator();
		
		Otp otp = Otp.builder()
				.otpHash(OtpUtils.hash(rawOtp))
				.type(request.getType())
				.referenceId(String.valueOf(request.getReferenceId()))
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.attempts(0)
				.maxAttempts(3)
				.status(OtpStatus.PENDING)
				.build();
		
		MessageEvent event = MessageEvent.builder()
				.otpNumber(rawOtp)
				.email(user.getEmail())
				.type(request.getType())
				.build();
		
		otpProducer.dispatchResponseWithRetry(event);
		otpRepo.save(otp);
		
		return true;
	}

	public OtpValidateResponse validateOtp(OtpValidateRequest request) {
		Otp otp = otpRepo.findByReferenceIdAndType(request.getReferenceId(), request.getType()).orElse(null);
		
		OtpValidateResponse otpResponse = OtpValidateResponse.builder()
				.referenceId(request.getReferenceId())
				.build();
		
		if (otp != null) {
			if (otp.getStatus() == OtpStatus.PENDING) {
				if (otp.getExpiresAt().isBefore(LocalDateTime.now()) || otp.getExpiresAt().isEqual(LocalDateTime.now())) {
					otp.setStatus(OtpStatus.EXPIRED);
				} else if (otp.getAttempts() >= otp.getMaxAttempts()) {
					otp.setStatus(OtpStatus.MAX_ATTEMPTS);
				} else {
					int attempts = otp.getAttempts() + 1;
					otp.setAttempts(attempts);
					
					String reqHashedOtp = OtpUtils.hash((String.valueOf(request.getOtpValue())));
					
					if (otp.getOtpHash().equals(reqHashedOtp)) {
						otp.setStatus(OtpStatus.VERIFIED);
						otpResponse.setValidated(true);
						otpResponse.setMessage("OTP verfied successfully.");
						return otpResponse;
					} else if (otp.getExpiresAt().isBefore(LocalDateTime.now()) || otp.getExpiresAt().isEqual(LocalDateTime.now())) {
						otp.setStatus(OtpStatus.EXPIRED);
					} else if (otp.getAttempts() >= otp.getMaxAttempts()) {
						otp.setStatus(OtpStatus.MAX_ATTEMPTS);						
					}
				}				
			} 
			
			if (otp.getStatus() == OtpStatus.MAX_ATTEMPTS) {
				otpResponse.setMessage("Max attempts crossed for this OTP.");
			} else if (otp.getStatus() == OtpStatus.VERIFIED) {
				otpResponse.setMessage("OTP already used.");
			} else if (otp.getStatus() == OtpStatus.EXPIRED) {
				otpResponse.setMessage("OTP has expired.");
			}
			otpRepo.save(otp);
		}
		otpResponse.setValidated(false);
		return otpResponse;
	}
}
