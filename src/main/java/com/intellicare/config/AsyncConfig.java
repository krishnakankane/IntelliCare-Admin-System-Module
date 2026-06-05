package com.intellicare.config;

import com.intellicare.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("intellicare-async-");
        executor.initialize();
        return executor;
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    public static class TokenCleanupScheduler {

        private final RefreshTokenRepository refreshTokenRepository;

        /** Runs every day at 03:00 UTC — removes expired/revoked refresh tokens */
        @Scheduled(cron = "0 0 3 * * *")
        @Transactional
        public void cleanupExpiredTokens() {
            int deleted = refreshTokenRepository.deleteExpiredAndRevokedTokens();
            log.info("Token cleanup: deleted {} expired/revoked refresh tokens", deleted);
        }
    }
}
