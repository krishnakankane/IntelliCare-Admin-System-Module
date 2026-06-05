package com.intellicare.service.impl;

import com.intellicare.entity.NotificationTemplate;
import com.intellicare.repository.NotificationTemplateRepository;
import com.intellicare.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final NotificationTemplateRepository templateRepository;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.ai.mock-enabled:true}")
    private boolean mockEnabled;

    @Override
    public String send(String to, String subject, String body) {
        if (mockEnabled) {
            log.info("[MOCK EMAIL] To={} Subject='{}' Body={}", to, subject, body.substring(0, Math.min(80, body.length())));
            return "mock-email-ref-" + System.currentTimeMillis();
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            String ref = "email-" + System.currentTimeMillis();
            log.info("Email sent to={} ref={}", to, ref);
            return ref;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String sendTemplate(String to, String templateName, Map<String, String> variables) {
        NotificationTemplate template = templateRepository.findByNameAndIsActiveTrue(templateName)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateName));

        String subject = resolveVariables(template.getSubject() != null ? template.getSubject() : "(no subject)", variables);
        String body    = resolveVariables(template.getBody(), variables);

        return send(to, subject, body);
    }

    private String resolveVariables(String template, Map<String, String> variables) {
        if (variables == null) return template;
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
