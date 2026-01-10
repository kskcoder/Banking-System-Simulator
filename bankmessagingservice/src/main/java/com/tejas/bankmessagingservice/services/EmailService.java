package com.tejas.bankmessagingservice.services;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.tejas.bankmessagingservice.configurations.MailProperties;
import com.tejas.bankingcommon.dto.MessageType;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final EmailStatusService emailStatusService;
    private final Environment environment;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;
    
    private String fromEmail = "no-reply@bankingsystem.com";
    private String senderName = "Banking System Simulator";
    
    @PostConstruct
    public void initEmailProperties() {
        try {
            String username = environment.getProperty("spring.mail.username", "no-reply@bankingsystem.com");
            if (username != null && !username.contains("${") && !username.isBlank()) {
                this.fromEmail = username;
            }
            
            String sender = environment.getProperty("mail.sender.name", "Banking System Simulator");
            if (sender != null && !sender.contains("${") && !sender.isBlank()) {
                this.senderName = sender;
            }
        } catch (Exception e) {
            log.debug("Could not resolve email properties, using defaults", e);
        }
    }
    
    public void sendOtpEmail(String to, String otp) {
        if (!emailStatusService.isEmailEnabled() || mailSender == null) {
            logToConsoleOtpEmail(to, otp);
            return;
        }
        
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
            log.debug("OTP email sent successfully to {}", to);

        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}. Falling back to console logging.", to, ex);
            logToConsoleOtpEmail(to, otp);
        }
    }
    
    public void sendTransactionEmail(String to, MessageType type, String messageContent) {
        if (!emailStatusService.isEmailEnabled() || mailSender == null) {
            logToConsoleTransactionEmail(to, type, messageContent);
            return;
        }
        
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
            log.debug("Transaction email sent successfully to {}", to);

        } catch (Exception ex) {
            log.error("Failed to send transaction email to {}. Falling back to console logging.", to, ex);
            logToConsoleTransactionEmail(to, type, messageContent);
        }
    }
    
    private void logToConsoleOtpEmail(String to, String otp) {
        String safeFromEmail = fromEmail != null ? fromEmail : "no-reply@bankingsystem.com";
        String safeSenderName = senderName != null ? senderName : "Banking System Simulator";
        
        String fromLine = String.format("From: %s (%s)", safeFromEmail, safeSenderName);
        String toLine = String.format("To:   %s", to);
        String subjectLine = "Subject: Your Payment Verification OTP";
        
        int maxWidth = 80;
        String fromDisplay = fromLine.length() > maxWidth - 4 ? fromLine.substring(0, maxWidth - 7) + "..." : fromLine;
        String toDisplay = toLine.length() > maxWidth - 4 ? toLine.substring(0, maxWidth - 7) + "..." : toLine;
        String subjectDisplay = subjectLine.length() > maxWidth - 4 ? subjectLine.substring(0, maxWidth - 7) + "..." : subjectLine;
        
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("\n");
        emailContent.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        emailContent.append("║                          EMAIL (CONSOLE MODE)                                ║\n");
        emailContent.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        emailContent.append(String.format("║ %-" + (maxWidth - 2) + "s ║\n", fromDisplay));
        emailContent.append(String.format("║ %-" + (maxWidth - 2) + "s ║\n", toDisplay));
        emailContent.append(String.format("║ %-" + (maxWidth - 2) + "s ║\n", subjectDisplay));
        emailContent.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        emailContent.append("║                                                                              ║\n");
        emailContent.append(String.format("║                          Your OTP is: %-30s ║\n", otp));
        emailContent.append("║                                                                              ║\n");
        emailContent.append("║                          Valid for 5 minutes.                                ║\n");
        emailContent.append("║                                                                              ║\n");
        emailContent.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
        emailContent.append("\n");
        
        log.info(emailContent.toString());
    }
    
    private void logToConsoleTransactionEmail(String to, MessageType type, String messageContent) {
        String[] messageArray = messageContent.split(",");
        String accountId = messageArray[0];
        String amount = messageArray[1];
        String balance = messageArray[2];
        String operation = type.equals(MessageType.CREDIT) ? "credited to" : "debited from";
        String subject = "Your account " + accountId + " has been " + (type.equals(MessageType.CREDIT) ? "credited." : "debited.");
        
        String safeFromEmail = fromEmail != null ? fromEmail : "no-reply@bankingsystem.com";
        String safeSenderName = senderName != null ? senderName : "Banking System Simulator";
        
        int maxWidth = 80;
        String fromLine = String.format("From: %s (%s)", safeFromEmail, safeSenderName);
        String toLine = String.format("To:   %s", to);
        String subjectLine = String.format("Subject: %s", subject);
        
        String fromDisplay = fromLine.length() > maxWidth - 4 ? fromLine.substring(0, maxWidth - 7) + "..." : fromLine;
        String toDisplay = toLine.length() > maxWidth - 4 ? toLine.substring(0, maxWidth - 7) + "..." : toLine;
        String subjectDisplay = subjectLine.length() > maxWidth - 4 ? subjectLine.substring(0, maxWidth - 7) + "..." : subjectLine;
        
        String amountLine = String.format("Amount %s has been %s your Account %s", amount, operation, accountId);
        String balanceLine = String.format("Current balance is %s", balance);
        
        int contentWidth = maxWidth - 4;
        String amountDisplay = amountLine.length() > contentWidth ? amountLine.substring(0, contentWidth - 3) + "..." : amountLine;
        String balanceDisplay = balanceLine.length() > contentWidth ? balanceLine.substring(0, contentWidth - 3) + "..." : balanceLine;
        
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("\n");
        emailContent.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        emailContent.append("║                          EMAIL (CONSOLE MODE)                                ║\n");
        emailContent.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        emailContent.append(String.format("║ %-" + (maxWidth - 2) + "s ║\n", fromDisplay));
        emailContent.append(String.format("║ %-" + (maxWidth - 2) + "s ║\n", toDisplay));
        emailContent.append(String.format("║ %-" + (maxWidth - 2) + "s ║\n", subjectDisplay));
        emailContent.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        emailContent.append("║                                                                              ║\n");
        emailContent.append(String.format("║  %-" + contentWidth + "s  ║\n", amountDisplay));
        emailContent.append("║                                                                              ║\n");
        emailContent.append(String.format("║  %-" + contentWidth + "s  ║\n", balanceDisplay));
        emailContent.append("║                                                                              ║\n");
        emailContent.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
        emailContent.append("\n");
        
        log.info(emailContent.toString());
    }

    
}
