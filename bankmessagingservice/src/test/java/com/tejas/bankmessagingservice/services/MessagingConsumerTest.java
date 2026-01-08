package com.tejas.bankmessagingservice.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.tejas.bankingcommon.dto.MessageEvent;
import com.tejas.bankingcommon.dto.MessageType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessagingConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MessagingConsumer messagingConsumer;

    private MessageEvent testMessageEvent;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        
        lenient().doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
        lenient().doNothing().when(emailService).sendTransactionEmail(anyString(), any(MessageType.class), anyString());
    }

    @Test
    void testSendMessage_Credit() {
        testMessageEvent = MessageEvent.builder()
                .email(testEmail)
                .type(MessageType.CREDIT)
                .message("AC123456789,1000.0,5000.0")
                .build();

        messagingConsumer.sendMessage(testMessageEvent);

        verify(emailService).sendTransactionEmail(eq(testEmail), eq(MessageType.CREDIT), eq("AC123456789,1000.0,5000.0"));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void testSendMessage_Debit() {
        testMessageEvent = MessageEvent.builder()
                .email(testEmail)
                .type(MessageType.DEBIT)
                .message("AC123456789,500.0,4500.0")
                .build();

        messagingConsumer.sendMessage(testMessageEvent);

        verify(emailService).sendTransactionEmail(eq(testEmail), eq(MessageType.DEBIT), eq("AC123456789,500.0,4500.0"));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void testSendMessage_PaymentOtp() {
        testMessageEvent = MessageEvent.builder()
                .email(testEmail)
                .type(MessageType.PAYMENT_OTP)
                .otpNumber("123456")
                .build();

        messagingConsumer.sendMessage(testMessageEvent);

        verify(emailService).sendOtpEmail(eq(testEmail), eq("123456"));
        verify(emailService, never()).sendTransactionEmail(anyString(), any(MessageType.class), anyString());
    }

    @Test
    void testSendMessage_LoginOtp() {
        testMessageEvent = MessageEvent.builder()
                .email(testEmail)
                .type(MessageType.LOGIN_OTP)
                .otpNumber("654321")
                .build();

        messagingConsumer.sendMessage(testMessageEvent);

        verify(emailService).sendOtpEmail(eq(testEmail), eq("654321"));
        verify(emailService, never()).sendTransactionEmail(anyString(), any(MessageType.class), anyString());
    }

    @Test
    void testSendMessage_RegisterOtp() {
        testMessageEvent = MessageEvent.builder()
                .email(testEmail)
                .type(MessageType.REGISTER_OTP)
                .otpNumber("789012")
                .build();

        messagingConsumer.sendMessage(testMessageEvent);

        verify(emailService).sendOtpEmail(eq(testEmail), eq("789012"));
        verify(emailService, never()).sendTransactionEmail(anyString(), any(MessageType.class), anyString());
    }
}
