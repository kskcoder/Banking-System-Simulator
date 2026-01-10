package com.tejas.bankmessagingservice.services;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Getter
public class EmailStatusService {
    
    private boolean emailEnabled = false;
    
    public void setEmailEnabled(boolean enabled) {
        this.emailEnabled = enabled;
        if (!enabled) {
            log.warn("Email service is DISABLED. Email messages will be logged to console instead of being sent.");
        } else {
            log.info("Email service is ENABLED. Emails will be sent via SMTP.");
        }
    }
    
    public boolean isEmailEnabled() {
        return emailEnabled;
    }
}
