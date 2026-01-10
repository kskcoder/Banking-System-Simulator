package com.tejas.bankmessagingservice.configurations;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.tejas.bankmessagingservice.services.EmailStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class MailConfigurationValidator implements ApplicationListener<ApplicationReadyEvent> {
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private MailProperties mailProperties;
    private final EmailStatusService emailStatusService;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (mailProperties == null) {
            log.warn("=== EMAIL CONFIGURATION NOT FOUND ===");
            log.warn("MailProperties bean is not available. Email functionality will be DISABLED.");
            log.warn("Email messages will be logged to console instead.");
            emailStatusService.setEmailEnabled(false);
            return;
        }
        
        List<String> validationErrors = validateMailConfiguration();
        
        if (!validationErrors.isEmpty()) {
            log.warn("=== EMAIL CONFIGURATION VALIDATION FAILED ===");
            log.warn("The following required email configuration properties are missing or invalid:");
            validationErrors.forEach(error -> log.warn("  - {}", error));
            log.warn("Email functionality will be DISABLED. Email messages will be logged to console instead.");
            log.warn("To enable email sending, please configure the required properties:");
            log.warn("  - spring.mail.host");
            log.warn("  - spring.mail.port (must be > 0)");
            log.warn("  - spring.mail.username");
            log.warn("  - spring.mail.password");
            log.warn("  - spring.mail.properties.mail.smtp.auth (must be true)");
            log.warn("  - spring.mail.properties.mail.smtp.starttls.enable (must be true)");
            log.warn("These properties can be set via:");
            log.warn("  1. Config Server (GitHub repository)");
            log.warn("  2. Environment variables");
            log.warn("  3. Docker Compose .env files");
            log.warn("=============================================");
            
            emailStatusService.setEmailEnabled(false);
        } else {
            log.info("Email configuration validated successfully. Email service is enabled.");
            log.info("SMTP Host: {}", mailProperties.getHost());
            log.info("SMTP Port: {}", mailProperties.getPort());
            log.info("SMTP Username: {}", mailProperties.getUsername());
            log.info("SMTP Auth Enabled: {}", mailProperties.isSmtpAuthEnabled());
            log.info("STARTTLS Enabled: {}", mailProperties.isStarttlsEnabled());
            
            emailStatusService.setEmailEnabled(true);
        }
    }
    
    private List<String> validateMailConfiguration() {
        List<String> errors = new ArrayList<>();
        
        if (mailProperties.getHost() == null || mailProperties.getHost().isBlank() || containsUnresolvedPlaceholder(mailProperties.getHost())) {
            errors.add("spring.mail.host is blank, missing, or contains unresolved placeholder");
        }
        
        if (mailProperties.getPort() == null || mailProperties.getPort() <= 0) {
            errors.add("spring.mail.port is missing or not greater than 0");
        }
        
        if (mailProperties.getUsername() == null || mailProperties.getUsername().isBlank() || containsUnresolvedPlaceholder(mailProperties.getUsername())) {
            errors.add("spring.mail.username is blank, missing, or contains unresolved placeholder");
        }
        
        if (mailProperties.getPassword() == null || mailProperties.getPassword().isBlank() || containsUnresolvedPlaceholder(mailProperties.getPassword())) {
            errors.add("spring.mail.password is blank, missing, or contains unresolved placeholder");
        }
        
        if (!mailProperties.isSmtpAuthEnabled()) {
            errors.add("spring.mail.properties.mail.smtp.auth is not set to true");
        }
        
        if (!mailProperties.isStarttlsEnabled()) {
            errors.add("spring.mail.properties.mail.smtp.starttls.enable is not set to true");
        }
        
        return errors;
    }
    
    private boolean containsUnresolvedPlaceholder(String value) {
        return value != null && value.contains("${");
    }
}
