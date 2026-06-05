package com.intellicare.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /** HMAC-SHA512 secret — must be at least 64 chars in production */
    private String secret;

    /** Access token lifetime in milliseconds (default 15 min) */
    private long expirationMs = 900_000L;

    /** Refresh token lifetime in milliseconds (default 7 days) */
    private long refreshExpirationMs = 604_800_000L;
}
