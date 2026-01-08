package com.tejas.bankmessagingservice.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.MessageType;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${mail.sender.name:Banking System Simulator}")
    private String senderName;
    
    public void sendOtpEmail(String to, String otp) {
        try {            
            ClassPathResource resource = new ClassPathResource("templates/otpTemplate.html");
            String content = new String(resource.getInputStream().readAllBytes());
            
            content = content.replace("{{OTP}}", otp);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(new InternetAddress(fromEmail, senderName));
            helper.setTo(to);
            helper.setSubject("Your Payment Verification OTP");
            helper.setText(content, true);

            mailSender.send(message);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void sendTransactionEmail(String to, MessageType type, String messageContent) {
    	String[] messageArray = messageContent.split(",");
    	String accountId = messageArray[0];
    	String amount = messageArray[1];
    	String balance = messageArray[2];
        try {
            ClassPathResource resource = new ClassPathResource("templates/transactionTemplate.html");
            String content = new String(resource.getInputStream().readAllBytes());
            
            content = content.replace("{{AMOUNT}}", amount).replace("{{OPERATION}}", type.equals(MessageType.CREDIT) ? "credited to" : "debited from").replace("{{ACCOUNT}}", accountId).replace("{{BALANCE}}", balance);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(new InternetAddress(fromEmail, senderName));
            helper.setTo(to);
            helper.setSubject("Your account "+accountId+" has been " +(type.equals(MessageType.CREDIT) ? "credited." : "debited."));
            helper.setText(content, true);

            mailSender.send(message);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    
}
