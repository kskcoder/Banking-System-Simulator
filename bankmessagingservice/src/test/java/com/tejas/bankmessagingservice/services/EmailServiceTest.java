package com.tejas.bankmessagingservice.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.tejas.bankingcommon.dto.MessageType;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private String testEmail;
    private String testOtp;
    private String testFromEmail;
    private String testSenderName;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testOtp = "123456";
        testFromEmail = "noreply@bank.com";
        testSenderName = "Banking System Simulator";

        ReflectionTestUtils.setField(emailService, "fromEmail", testFromEmail);
        ReflectionTestUtils.setField(emailService, "senderName", testSenderName);

        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        lenient().doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendOtpEmail_Success() {
        assertDoesNotThrow(() -> emailService.sendOtpEmail(testEmail, testOtp));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendOtpEmail_ExceptionHandled() {
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendOtpEmail(testEmail, testOtp));
    }

    @Test
    void testSendTransactionEmail_Credit_Success() {
        String messageContent = "AC123456789,1000.0,5000.0";

        assertDoesNotThrow(() -> emailService.sendTransactionEmail(testEmail, MessageType.CREDIT, messageContent));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendTransactionEmail_Debit_Success() {
        String messageContent = "AC123456789,500.0,4500.0";

        assertDoesNotThrow(() -> emailService.sendTransactionEmail(testEmail, MessageType.DEBIT, messageContent));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendTransactionEmail_ExceptionHandled() {
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));
        String messageContent = "AC123456789,1000.0,5000.0";

        assertDoesNotThrow(() -> emailService.sendTransactionEmail(testEmail, MessageType.CREDIT, messageContent));
    }
}
