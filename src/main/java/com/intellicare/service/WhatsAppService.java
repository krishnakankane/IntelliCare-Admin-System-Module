package com.intellicare.service;

public interface WhatsAppService {
    String sendTextMessage(String toPhoneNumber, String message);
    String sendTemplateMessage(String toPhoneNumber, String templateName, java.util.Map<String, String> parameters);
}
