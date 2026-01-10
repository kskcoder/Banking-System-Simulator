package com.tejas.bankmessagingservice.configurations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.tejas.bankmessagingservice.services.EmailStatusService;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MailSenderConfiguration {

    @Bean
    @ConditionalOnBean(MailProperties.class)
    public JavaMailSender javaMailSender(MailProperties mailProperties, EmailStatusService emailStatusService) {
        if (!isMailConfigurationValid(mailProperties)) {
            log.warn("Mail configuration contains unresolved placeholders or invalid values. JavaMailSender will not be created.");
            emailStatusService.setEmailEnabled(false);
            return null;
        }
        
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(mailProperties.getHost());
            mailSender.setPort(mailProperties.getPort());
            mailSender.setUsername(mailProperties.getUsername());
            mailSender.setPassword(mailProperties.getPassword());
            
            java.util.Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", mailProperties.isSmtpAuthEnabled());
            props.put("mail.smtp.starttls.enable", mailProperties.isStarttlsEnabled());
            
            log.info("JavaMailSender created successfully with host: {}", mailProperties.getHost());
            return mailSender;
        } catch (Exception e) {
            log.warn("Failed to create JavaMailSender: {}", e.getMessage());
            emailStatusService.setEmailEnabled(false);
            return null;
        }
    }
    
    private boolean isMailConfigurationValid(MailProperties mailProperties) {
        if (mailProperties == null) {
            return false;
        }
        
        String host = mailProperties.getHost();
        String username = mailProperties.getUsername();
        String password = mailProperties.getPassword();
        Integer port = mailProperties.getPort();
        
        if (host == null || host.isBlank() || host.contains("${")) {
            return false;
        }
        
        if (username == null || username.isBlank() || username.contains("${")) {
            return false;
        }
        
        if (password == null || password.isBlank() || password.contains("${")) {
            return false;
        }
        
        if (port == null || port <= 0) {
            return false;
        }
        
        if (!mailProperties.isSmtpAuthEnabled() || !mailProperties.isStarttlsEnabled()) {
            return false;
        }
        
        return true;
    }
}
