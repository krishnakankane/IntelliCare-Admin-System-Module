package com.intellicare.service.impl;

import com.intellicare.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    @Value("${app.whatsapp.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${app.whatsapp.api-url}")
    private String apiUrl;

    @Value("${app.whatsapp.access-token}")
    private String accessToken;

    @Value("${app.whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Override
    public String sendTextMessage(String toPhoneNumber, String message) {
        if (mockEnabled) {
            log.info("[MOCK WHATSAPP] To={} Message='{}'", toPhoneNumber, message);
            return "mock-wa-ref-" + System.currentTimeMillis();
        }
        // Production: WhatsApp Cloud API
        // POST /v18.0/{phone-number-id}/messages
        log.warn("WhatsApp Cloud API not wired up — configure credentials and implement HTTP call");
        throw new UnsupportedOperationException("WhatsApp integration not configured");
    }

    @Override
    public String sendTemplateMessage(String toPhoneNumber, String templateName, Map<String, String> parameters) {
        if (mockEnabled) {
            log.info("[MOCK WHATSAPP TEMPLATE] To={} Template={} Params={}", toPhoneNumber, templateName, parameters);
            return "mock-wa-tmpl-ref-" + System.currentTimeMillis();
        }
        throw new UnsupportedOperationException("WhatsApp template messaging not configured");
    }
}
