package com.intellicare.service;

public interface SmsService {
    String send(String toPhoneNumber, String message);
}
