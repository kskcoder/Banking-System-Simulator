package com.tejas.bankmessagingservice.services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    
    public void sendOtpEmail(String to, String otp) {

        try {            
            ClassPathResource resource = new ClassPathResource("templates/otpTemplate.html");
            String content = new String(resource.getInputStream().readAllBytes());
            
            content = content.replace("{{OTP}}", otp);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Your Verification OTP");
            helper.setText(content, true);

            mailSender.send(message);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
