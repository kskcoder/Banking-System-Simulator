package com.tejas.bankauthservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankauthservice.models.ChangePasswordRequest;
import com.tejas.bankauthservice.models.LoginRequest;
import com.tejas.bankauthservice.models.SignupRequest;
import com.tejas.bankauthservice.models.User;
import com.tejas.bankauthservice.services.AuthService;
import com.tejas.bankingcommon.dto.ContactDetails;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService service;
	
	@PostMapping("/signup")
	@Operation(summary = "User signup", description = "Register a new user account")
	public ResponseEntity<User> saveUser(@Valid @RequestBody SignupRequest req) {
		return ResponseEntity.ok().body(service.signupUser(req));
	}
	
	@PostMapping("/loginuser")
	@Operation(summary = "User login", description = "Authenticate user and get JWT token")
	public ResponseEntity<String> loginUser(@Valid @RequestBody LoginRequest req) {
		return ResponseEntity.ok().body(service.verifyUser(req));
	}
	
	@PutMapping("/changepassword/{userId}")
	@Operation(summary = "Change password", description = "Update user password")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "userId",
		in = ParameterIn.PATH,
		required = true,
		description = "User ID for password change",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<String> changePassword(@Valid @PathVariable Long userId, @Valid @RequestBody ChangePasswordRequest request) {
		return ResponseEntity.ok().body(service.changePassword(userId, request));
	}	
	
	@PutMapping("/makeadmin/{userId}")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "userId",
		in = ParameterIn.PATH,
		required = true,
		description = "User ID to grant admin privileges",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<String> makeAdmin(@PathVariable Long userId) {
		return ResponseEntity.ok().body(service.makeAdmin(userId));
	}
	
	@DeleteMapping("/deleteuser/{userId}")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "userId",
		in = ParameterIn.PATH,
		required = true,
		description = "User ID to delete",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<String> deleteUser(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.deleteUser(userId));
	}	
	
	@Hidden
	@GetMapping("/getcontact/{userId}")
	@Parameter(
		name = "userId",
		in = ParameterIn.PATH,
		required = true,
		description = "User ID to get contact details",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<ContactDetails> getContact(@Valid @PathVariable Long userId) {
		return ResponseEntity.ok().body(service.getContact(userId));
	}
	
	@Hidden
	@PostMapping("/sendotp/{userId}")
	@Parameter(
		name = "userId",
		in = ParameterIn.PATH,
		required = true,
		description = "User ID to send OTP",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "OTP request details",
		required = true,
		content = @Content(
			schema = @Schema(implementation = OtpRequestDTO.class)
		)
	)
	public ResponseEntity<Boolean> sendPaymentOtp(@Valid @PathVariable Long userId, @Valid @RequestBody OtpRequestDTO request) {
		return ResponseEntity.ok().body(service.sendOtp(request));
	}
	
	@Hidden
	@PostMapping("/submitotp")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "OTP validation details",
		required = true,
		content = @Content(
			schema = @Schema(implementation = OtpValidateRequest.class)
		)
	)
	public ResponseEntity<OtpValidateResponse> validateOtp(@Valid @RequestBody OtpValidateRequest request) {
		return ResponseEntity.ok().body(service.validateOtp(request));
	}
}
