package com.intellicare.service;

/**
 * Email delivery service contract.
 */
public interface EmailService {
    /**
     * Send a plain-text or HTML email.
     *
     * @param to      recipient address
     * @param subject subject line
     * @param body    email body (HTML allowed)
     * @return external message-id / reference, or "mock-ref" in mock mode
     */
    String send(String to, String subject, String body);

    /**
     * Send a template-based email with variable substitution.
     */
    String sendTemplate(String to, String templateName, java.util.Map<String, String> variables);
}
