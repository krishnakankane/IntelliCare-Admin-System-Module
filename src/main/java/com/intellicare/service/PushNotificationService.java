package com.intellicare.service;

public interface PushNotificationService {
    String send(Long userId, String title, String body);
}
