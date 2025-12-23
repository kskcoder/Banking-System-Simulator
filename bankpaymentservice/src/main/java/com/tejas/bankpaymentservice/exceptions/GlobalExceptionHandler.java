package com.tejas.bankpaymentservice.exceptions;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tejas.bankingcommon.dto.ApiError;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler
	public ResponseEntity<ApiError> handleForbidden(ForbiddenException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.FORBIDDEN.value(),
				"FORBIDDEN_ACCESS",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleBadRequest(BadRequestException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				"BAD_REQUEST",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handlePaymentNotFound(NotFoundException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.NOT_FOUND.value(),
				"PAYMENT_NOT_FOUND",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleNoContentException(NoContentException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.NO_CONTENT.value(),
				"NO_CONTENT",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleGeneralGeneration(GeneralServerException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"SERVER_ERROR",
				"Something went wrong",
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
