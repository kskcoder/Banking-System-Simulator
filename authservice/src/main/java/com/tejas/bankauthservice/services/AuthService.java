package com.tejas.bankauthservice.services;

import java.time.LocalDateTime;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tejas.bankauthservice.exceptions.AlreadyUsedException;
import com.tejas.bankauthservice.models.ChangePasswordRequest;
import com.tejas.bankauthservice.models.LoginRequest;
import com.tejas.bankauthservice.models.Otp;
import com.tejas.bankauthservice.models.SignupRequest;
import com.tejas.bankauthservice.models.User;
import com.tejas.bankauthservice.repositories.AuthRepo;
import com.tejas.bankauthservice.repositories.OtpRepo;
import com.tejas.bankauthservice.utils.AuthUtils;
import com.tejas.bankauthservice.utils.OtpUtils;
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
	private final BCryptPasswordEncoder passwordEncoder;
	private final CacheManager cacheManager;
	
	private void evictUserCache(String username) {
		Cache cache = cacheManager.getCache("user_details");
		if (cache != null && username != null) {
			cache.evict(username);
		}
	}

	public User signupUser(SignupRequest req) {
		User user1 = new User();
		
		user1 = repo.getByUsername(req.getUsername()).orElse(null);
		if (user1 != null) {
			throw new AlreadyUsedException("Username: "+req.getUsername());
		} 
		
		user1 = repo.getByEmail(req.getEmail()).orElse(null);
		if (user1 != null) {
			throw new AlreadyUsedException("Email: "+req.getEmail());
		}
		
		User user = new User();
		user.setUsername(req.getUsername());
		user.setEmail(req.getEmail());
		user.setPassword(passwordEncoder.encode(req.getPassword()));
		user.setPhone(req.getPhone());
		user.setRole(UserType.USER);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		try {
			User savedUser = repo.save(user);
			evictUserCache(savedUser.getUsername());
			return savedUser;
		} catch (Exception e) {
			throw new GeneralServerException();
		}		
	}

	public String verifyUser(LoginRequest req) {
		String username = req.getUsername();
		String password = req.getPassword();
		
		User user = repo.getByUsername(username).orElse(null);
		
		if (user == null) {throw new NotFoundException("User not found");}
		
		try {
			Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
			
			if (authentication.isAuthenticated()) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				return jwtService.generateToken(user.getId(), userDetails);
			} else {
				throw new UnauthorizedException("Incorrect Password");
			}
		} catch (BadCredentialsException e) {
			throw new UnauthorizedException("Incorrect Password");
		} catch (IllegalArgumentException e) {
			System.out.println("IllegalArgumentException: " + e.getMessage());
			throw new UnauthorizedException("Authentication configuration error");
		} catch (InternalAuthenticationServiceException e) {
			System.out.println("InternalAuthenticationServiceException: " + e.getMessage());
			throw new UnauthorizedException("Authentication failed");
		} catch (Exception e) {
			System.out.println("Exception type: " + e.getClass().getName());
			System.out.println("Exception message: " + (e.getMessage() != null ? e.getMessage() : "null"));
			e.printStackTrace();
			throw new UnauthorizedException("Authentication failed");
		}
	}
	
	public String changePassword(long userId, ChangePasswordRequest request) {
		User user = repo.findById(userId)
			.orElseThrow(() -> 
				new NotFoundException("User not found")
			);
		
		if (user == null) {throw new NotFoundException("User not found");}
		String id = AuthUtils.getUserId();
		
		if (String.valueOf(userId).equals(id) || AuthUtils.isAdmin()) {
			String encodedOldPassword = user.getPassword();
			String username = user.getUsername();
			if (passwordEncoder.matches(request.getOldPassword(), encodedOldPassword)) {
				if (passwordEncoder.matches(request.getNewPassword(), encodedOldPassword)) {
					throw new BadRequestException("Old and new passwords cannot be same.");
				} else {
					user.setPassword(passwordEncoder.encode(request.getNewPassword()));
					repo.save(user);
					evictUserCache(username);
				}				
			} else {
				throw new BadRequestException("Incorrect old password.");
			}
		} else {
			throw new UnauthorizedException("You do not have access to this account.");
		}
		
		return "Password changed successfully.";
	}	

	public String makeAdmin(long userId) {
		if (AuthUtils.isAdmin()) {
			User user = repo.getById(userId)
					.orElseThrow(() -> 
						new NotFoundException("User not found")
					);
			
			if (user.getRole().equals(UserType.ADMIN)) {
				throw new BadRequestException("User is already an admin.");
			} else {
				String username = user.getUsername();
				user.setRole(UserType.ADMIN);
				repo.save(user);
				evictUserCache(username);
				return "Changed role to admin successfully.";
			}
		} else {
			throw new UnauthorizedException("You do not have access to this account.");
		}		
	}

	public String deleteUser(long userId) {
		User user = repo.getById(userId)
			.orElseThrow(() -> 
				new NotFoundException("User not found")
			);
		
		if (user == null) {throw new NotFoundException("User not found");}
		String id = AuthUtils.getUserId();
		
		if (String.valueOf(userId).equals(id) || AuthUtils.isAdmin()) {
			String username = user.getUsername();
			repo.delete(user);
			evictUserCache(username);
		} else {
			throw new UnauthorizedException("You do not have access to this account.");
		}
				
		return "User deleted successfully.";
	}
	
	public ContactDetails getContact(Long userId) {
		if (AuthUtils.isAdmin()) {
			return cachedGetContact(userId);
		}
		
		String role = AuthUtils.getRole();
		if (role != null && role.equals(UserType.INTERNAL_SERVICE.toString())) {
			return cachedGetContact(userId);
		}
		
		String id = AuthUtils.getUserId();
		if (id != null && id.equals("INTERNAL_PAYMENT_SERVICE")) {
			return cachedGetContact(userId);
		}
		
		throw new UnauthorizedException("You do not have access to this account.");
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

	public Boolean userExists(Long userId) {
		String requestingUserId = AuthUtils.getUserId();
		
		if (AuthUtils.isAdmin() || String.valueOf(userId).equals(requestingUserId)) {
			return repo.findById(userId).isPresent();
		} else {
			throw new UnauthorizedException("You do not have access to this account.");
		}
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
