package com.tejas.bankmessagingservice.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "spring.mail", ignoreUnknownFields = true, ignoreInvalidFields = true)
@Data
public class MailProperties {
    
    private String host;
    
    private Integer port;
    
    private String username;
    
    private String password;
    
    private Properties properties = new Properties();
    
    @Data
    public static class Properties {
        private Mail mail = new Mail();
        
        @Data
        public static class Mail {
            private Smtp smtp = new Smtp();
            
            @Data
            public static class Smtp {
                private Boolean auth;
                private Starttls starttls = new Starttls();
                
                @Data
                public static class Starttls {
                    private Boolean enable;
                }
            }
        }
    }
    
    public boolean isSmtpAuthEnabled() {
        return properties != null 
            && properties.getMail() != null
            && properties.getMail().getSmtp() != null 
            && Boolean.TRUE.equals(properties.getMail().getSmtp().getAuth());
    }
    
    public boolean isStarttlsEnabled() {
        return properties != null 
            && properties.getMail() != null
            && properties.getMail().getSmtp() != null 
            && properties.getMail().getSmtp().getStarttls() != null
            && Boolean.TRUE.equals(properties.getMail().getSmtp().getStarttls().getEnable());
    }
}
