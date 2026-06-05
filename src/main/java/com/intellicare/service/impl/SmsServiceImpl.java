package com.intellicare.service.impl;

import com.intellicare.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${app.twilio.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${app.twilio.account-sid:mock-sid}")
    private String accountSid;

    @Value("${app.twilio.auth-token:mock-token}")
    private String authToken;

    @Value("${app.twilio.from-number:+15005550006}")
    private String fromNumber;

    @Override
    public String send(String toPhoneNumber, String message) {
        if (mockEnabled) {
            log.info("[MOCK SMS] To={} Message='{}'", toPhoneNumber, message);
            return "mock-sms-sid-" + System.currentTimeMillis();
        }

        // Production: Twilio REST API call
        // TwilioClient.init(accountSid, authToken);
        // Message msg = Message.creator(new PhoneNumber(toPhoneNumber),
        //     new PhoneNumber(fromNumber), message).create();
        // return msg.getSid();

        // Placeholder — replace with Twilio SDK when twilio-java is added to pom.xml
        log.warn("Twilio SDK not integrated — add com.twilio.sdk:twilio to pom.xml");
        throw new UnsupportedOperationException("Twilio SDK not integrated");
    }
}
