package com.intellicare.service.impl;

import com.intellicare.service.PushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    @Override
    public String send(Long userId, String title, String body) {
        // In-app push: stored via Notification entity with channel=IN_APP
        // For native push (FCM/APNs), integrate Firebase Admin SDK here
        log.info("[PUSH] userId={} title='{}' body='{}'", userId, title, body);
        return "push-ref-" + System.currentTimeMillis();
    }
}
