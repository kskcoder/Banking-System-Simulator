package com.tejas.bankpaymentservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tejas.bankingcommon.dto.CardVerificationRequest;
import com.tejas.bankingcommon.dto.CardVerificationResponse;
import com.tejas.bankingcommon.dto.MessageType;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;
import com.tejas.bankingcommon.dto.SubmitPaymentOtp;
import com.tejas.bankingcommon.dto.Transaction;
import com.tejas.bankingcommon.dto.TransferRequest;
import com.tejas.bankingcommon.enums.PaymentStatus;
import com.tejas.bankingcommon.enums.PaymentType;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankpaymentservice.feign.AccountInterface;
import com.tejas.bankpaymentservice.feign.AuthInterface;
import com.tejas.bankpaymentservice.feign.CardInterface;
import com.tejas.bankpaymentservice.feign.TransactionInterface;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.repositories.PaymentRepo;

import feign.FeignException;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentRepo repo;

    @Mock
    private CardInterface cardInt;

    @Mock
    private AccountInterface accInt;

    @Mock
    private AuthInterface authInt;

    @Mock
    private TransactionInterface trInt;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache paymentCache;

    @Mock
    private Cache allPaymentsCache;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private InitiatePaymentDTO testInitiateRequest;
    private String testVendorId;
    private String testFromAccount;
    private String testToAccount;
    private double testAmount;
    private long testAccountId;
    private long testUserId;

    @BeforeEach
    void setUp() {
        testVendorId = "VENDOR123";
        testFromAccount = "AC112345678901234567890";
        testToAccount = "AC198765432109876543210";
        testAmount = 1000.0;
        testAccountId = 1L;
        testUserId = 1L;

        testPayment = Payment.builder()
                .id(1L)
                .vendorId(testVendorId)
                .fromAccountNumber(testFromAccount)
                .toAccountNumber(testToAccount)
                .amount(testAmount)
                .type(PaymentType.CARD)
                .status(PaymentStatus.INITIATED)
                .build();

        testInitiateRequest = new InitiatePaymentDTO();
        testInitiateRequest.setToAccountNumber(testToAccount);
        testInitiateRequest.setAmount(testAmount);
        testInitiateRequest.setType(PaymentType.CARD);
        testInitiateRequest.setCardNumber("1234567890123456");
        testInitiateRequest.setCvv("123");
        testInitiateRequest.setExpiry("12/25");

        ReflectionTestUtils.setField(paymentService, "cacheManager", cacheManager);
        ReflectionTestUtils.setField(paymentService, "objectMapper", new ObjectMapper());

        lenient().when(cacheManager.getCache("payment_details")).thenReturn(paymentCache);
        lenient().when(cacheManager.getCache("all_payments")).thenReturn(allPaymentsCache);
        lenient().doNothing().when(paymentCache).evict(anyLong());
        lenient().doNothing().when(allPaymentsCache).clear();

        RequestContextHolder.setRequestAttributes(null);
    }

    @Test
    void testInitiateRequest_Success_Card() {
        mockRequestContext(testVendorId);
        when(repo.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        CardVerificationResponse cardResponse = CardVerificationResponse.builder()
                .validated(true)
                .accountId(testAccountId)
                .build();
        when(cardInt.verifyCard(any(CardVerificationRequest.class))).thenReturn(ResponseEntity.ok(cardResponse));
        when(accInt.getAccountNumberByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(testFromAccount));
        when(accInt.getuserIdByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(testUserId));
        when(authInt.sendPaymentOtp(eq(testUserId), any(OtpRequestDTO.class))).thenReturn(ResponseEntity.ok(true));

        ResponseEntity<PaymentResponse> result = paymentService.initiateRequest(testInitiateRequest);

        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(PaymentStatus.INITIATED, result.getBody().getStatus());
        assertEquals("OTP has been sent", result.getBody().getMessage());
        verify(repo, times(2)).save(any(Payment.class));
    }

    @Test
    void testInitiateRequest_CardVerificationFailed() {
        mockRequestContext(testVendorId);
        when(repo.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("Card verification failed");
        when(cardInt.verifyCard(any(CardVerificationRequest.class))).thenThrow(feignException);

        ResponseEntity<PaymentResponse> result = paymentService.initiateRequest(testInitiateRequest);

        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(PaymentStatus.FAILED, result.getBody().getStatus());
        assertEquals("Card verification failed", result.getBody().getMessage());
    }

    @Test
    void testInitiateRequest_UPI() {
        mockRequestContext(testVendorId);
        testInitiateRequest.setType(PaymentType.UPI);
        when(repo.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });

        ResponseEntity<PaymentResponse> result = paymentService.initiateRequest(testInitiateRequest);

        assertNotNull(result);
        assertNull(result.getBody());
    }

    @Test
    void testInitiateRequest_NoRequestAttributes() {
        RequestContextHolder.setRequestAttributes(null);

        assertThrows(GeneralServerException.class, () -> paymentService.initiateRequest(testInitiateRequest));
    }

    @Test
    void testInitiateRequest_SaveException() {
        mockRequestContext(testVendorId);
        when(repo.save(any(Payment.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(GeneralServerException.class, () -> paymentService.initiateRequest(testInitiateRequest));
    }

    @Test
    void testPostCardValidation_Success() {
        CardVerificationResponse cardResponse = CardVerificationResponse.builder()
                .validated(true)
                .accountId(testAccountId)
                .build();
        when(accInt.getAccountNumberByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(testFromAccount));
        when(accInt.getuserIdByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(testUserId));
        when(authInt.sendPaymentOtp(eq(testUserId), any(OtpRequestDTO.class))).thenReturn(ResponseEntity.ok(true));
        when(repo.save(any(Payment.class))).thenReturn(testPayment);

        ResponseEntity<PaymentResponse> result = paymentService.postCardValidation(testInitiateRequest, testPayment, cardResponse);

        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(PaymentStatus.INITIATED, result.getBody().getStatus());
        assertEquals("OTP has been sent", result.getBody().getMessage());
    }

    @Test
    void testPostCardValidation_CardNotValidated() {
        CardVerificationResponse cardResponse = CardVerificationResponse.builder()
                .validated(false)
                .accountId(testAccountId)
                .build();

        ResponseEntity<PaymentResponse> result = paymentService.postCardValidation(testInitiateRequest, testPayment, cardResponse);

        assertNotNull(result);
        assertNull(result.getBody());
    }

    @Test
    void testPostCardValidation_AccountNumberNull() {
        CardVerificationResponse cardResponse = CardVerificationResponse.builder()
                .validated(true)
                .accountId(testAccountId)
                .build();
        when(accInt.getAccountNumberByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(null));

        assertThrows(GeneralServerException.class, () -> paymentService.postCardValidation(testInitiateRequest, testPayment, cardResponse));
    }

    @Test
    void testPostCardValidation_UserIdNull() {
        CardVerificationResponse cardResponse = CardVerificationResponse.builder()
                .validated(true)
                .accountId(testAccountId)
                .build();
        when(accInt.getAccountNumberByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(testFromAccount));
        when(accInt.getuserIdByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(null));

        assertThrows(GeneralServerException.class, () -> paymentService.postCardValidation(testInitiateRequest, testPayment, cardResponse));
    }

    @Test
    void testPostCardValidation_FeignException() {
        CardVerificationResponse cardResponse = CardVerificationResponse.builder()
                .validated(true)
                .accountId(testAccountId)
                .build();
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("Account service error");
        when(accInt.getAccountNumberByAccountId(testAccountId)).thenThrow(feignException);
        when(repo.save(any(Payment.class))).thenReturn(testPayment);

        ResponseEntity<PaymentResponse> result = paymentService.postCardValidation(testInitiateRequest, testPayment, cardResponse);

        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(PaymentStatus.FAILED, result.getBody().getStatus());
        assertEquals("Account service error", result.getBody().getMessage());
    }

    @Test
    void testSubmitOtp_Success() {
        SubmitPaymentOtp otpRequest = new SubmitPaymentOtp();
        otpRequest.setPaymentId(1L);
        otpRequest.setOtp("123456");

        when(repo.findById(1L)).thenReturn(Optional.of(testPayment));
        OtpValidateResponse otpResponse = OtpValidateResponse.builder()
                .referenceId("1")
                .validated(true)
                .retried(false)
                .message("OTP validated")
                .build();
        when(authInt.validateOtp(any(OtpValidateRequest.class))).thenReturn(ResponseEntity.ok(otpResponse));
        when(trInt.transfer(any(TransferRequest.class))).thenReturn(ResponseEntity.ok(mock(Transaction.class)));
        when(repo.save(any(Payment.class))).thenReturn(testPayment);

        ResponseEntity<OtpValidateResponse> result = paymentService.submitOtp(otpRequest);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isValidated());
        assertEquals("OTP verified. Payment is processing.", result.getBody().getMessage());
    }

    @Test
    void testSubmitOtp_PaymentNotFound() {
        SubmitPaymentOtp otpRequest = new SubmitPaymentOtp();
        otpRequest.setPaymentId(1L);
        otpRequest.setOtp("123456");

        when(repo.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> paymentService.submitOtp(otpRequest));
        assertEquals("Incorrect Payment Id", exception.getMessage());
    }

    @Test
    void testSubmitOtp_PaymentFailed() {
        SubmitPaymentOtp otpRequest = new SubmitPaymentOtp();
        otpRequest.setPaymentId(1L);
        otpRequest.setOtp("123456");

        testPayment.setStatus(PaymentStatus.FAILED);
        when(repo.findById(1L)).thenReturn(Optional.of(testPayment));

        ResponseEntity<OtpValidateResponse> result = paymentService.submitOtp(otpRequest);

        assertNotNull(result);
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isValidated());
        assertEquals("Payment has failed, initiate new payment", result.getBody().getMessage());
    }

    @Test
    void testSubmitOtp_OtpValidationFailed() {
        SubmitPaymentOtp otpRequest = new SubmitPaymentOtp();
        otpRequest.setPaymentId(1L);
        otpRequest.setOtp("123456");

        when(repo.findById(1L)).thenReturn(Optional.of(testPayment));
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("OTP validation failed");
        when(authInt.validateOtp(any(OtpValidateRequest.class))).thenThrow(feignException);
        when(repo.save(any(Payment.class))).thenReturn(testPayment);

        ResponseEntity<OtpValidateResponse> result = paymentService.submitOtp(otpRequest);

        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isValidated());
        assertEquals("OTP validation failed", result.getBody().getMessage());
    }

    @Test
    void testSubmitOtp_OtpNotValidated() {
        SubmitPaymentOtp otpRequest = new SubmitPaymentOtp();
        otpRequest.setPaymentId(1L);
        otpRequest.setOtp("123456");

        when(repo.findById(1L)).thenReturn(Optional.of(testPayment));
        OtpValidateResponse otpResponse = OtpValidateResponse.builder()
                .referenceId("1")
                .validated(false)
                .retried(false)
                .message("Invalid OTP")
                .build();
        when(authInt.validateOtp(any(OtpValidateRequest.class))).thenReturn(ResponseEntity.ok(otpResponse));
        when(repo.save(any(Payment.class))).thenReturn(testPayment);

        ResponseEntity<OtpValidateResponse> result = paymentService.submitOtp(otpRequest);

        assertNotNull(result);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isValidated());
    }

    @Test
    void testSubmitOtp_OtpRetried() {
        SubmitPaymentOtp otpRequest = new SubmitPaymentOtp();
        otpRequest.setPaymentId(1L);
        otpRequest.setOtp("123456");

        when(repo.findById(1L)).thenReturn(Optional.of(testPayment));
        OtpValidateResponse otpResponse = OtpValidateResponse.builder()
                .referenceId("1")
                .validated(true)
                .retried(true)
                .message("OTP retried")
                .build();
        when(authInt.validateOtp(any(OtpValidateRequest.class))).thenReturn(ResponseEntity.ok(otpResponse));

        ResponseEntity<OtpValidateResponse> result = paymentService.submitOtp(otpRequest);

        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isValidated());
        assertTrue(result.getBody().isRetried());
    }

    @Test
    void testSubmitOtp_OtpResponseNull() {
        SubmitPaymentOtp otpRequest = new SubmitPaymentOtp();
        otpRequest.setPaymentId(1L);
        otpRequest.setOtp("123456");

        when(repo.findById(1L)).thenReturn(Optional.of(testPayment));
        when(authInt.validateOtp(any(OtpValidateRequest.class))).thenReturn(ResponseEntity.ok(null));

        assertThrows(GeneralServerException.class, () -> paymentService.submitOtp(otpRequest));
    }

    @Test
    void testValidOtp_Success() {
        when(trInt.transfer(any(TransferRequest.class))).thenReturn(ResponseEntity.ok(mock(Transaction.class)));
        when(repo.save(any(Payment.class))).thenReturn(testPayment);

        ResponseEntity<OtpValidateResponse> result = paymentService.validOtp(testPayment);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isValidated());
        assertEquals("OTP verified. Payment is processing.", result.getBody().getMessage());
        assertEquals(PaymentStatus.OTP_VERIFIED, testPayment.getStatus());
    }

    @Test
    void testValidOtp_TransferException() {
        FeignException feignException = mock(FeignException.class);
        when(feignException.contentUTF8()).thenReturn("Transfer failed");
        when(trInt.transfer(any(TransferRequest.class))).thenThrow(feignException);

        assertThrows(GeneralServerException.class, () -> paymentService.validOtp(testPayment));
    }

    @Test
    void testGetAllPayments_Success_AsAdmin() {
        mockRequestContextWithRole("ADMIN");
        List<Payment> payments = Collections.singletonList(testPayment);
        when(repo.findAll()).thenReturn(payments);

        List<Payment> result = paymentService.getAllPayments();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllPayments_Forbidden_NotAdmin() {
        mockRequestContextWithRole("USER");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> paymentService.getAllPayments());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
    }

    @Test
    void testGetAllPayments_Forbidden_NullRole() {
        mockRequestContextWithRole(null);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> paymentService.getAllPayments());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
    }

    @Test
    void testGetAllPayments_NoContent() {
        mockRequestContextWithRole("ADMIN");
        when(repo.findAll()).thenReturn(new ArrayList<>());

        NoContentException exception = assertThrows(NoContentException.class, () -> paymentService.getAllPayments());
        assertEquals("No payments found.", exception.getMessage());
    }

    @Test
    void testGetAllPayments_NoRequestAttributes() {
        RequestContextHolder.setRequestAttributes(null);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> paymentService.getAllPayments());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
    }

    @Test
    void testGetCachedPayment_Success() {
        when(repo.findById(1L)).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getCachedPayment(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetCachedPayment_NotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> paymentService.getCachedPayment(1L));
        assertEquals("Incorrect Payment Id", exception.getMessage());
    }

    @Test
    void testEvictPaymentCache() {
        when(cacheManager.getCache("payment_details")).thenReturn(paymentCache);
        doNothing().when(paymentCache).evict(1L);

        paymentService.evictPaymentCache(1L);

        verify(paymentCache).evict(1L);
    }

    @Test
    void testCalculateSignatureDemo_Success() {
        String requestBody = "{\"amount\":1000,\"toAccount\":\"AC123\"}";
        String vendorSecret = "secret123";
        String timestamp = "1234567890";

        String result = paymentService.calculateSignatureDemo(requestBody, vendorSecret, timestamp);

        assertNotNull(result);
    }

    @Test
    void testCalculateSignatureDemo_WithNullObjectMapper() {
        ReflectionTestUtils.setField(paymentService, "objectMapper", null);
        String requestBody = "{\"amount\":1000}";
        String vendorSecret = "secret123";
        String timestamp = "1234567890";

        String result = paymentService.calculateSignatureDemo(requestBody, vendorSecret, timestamp);

        assertNotNull(result);
    }

    private void mockRequestContext(String vendorId) {
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Vendor-Id")).thenReturn(vendorId);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    private void mockRequestContextWithRole(String role) {
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-User-Role")).thenReturn(role);
        RequestContextHolder.setRequestAttributes(attributes);
    }
}
