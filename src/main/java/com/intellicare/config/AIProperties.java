package com.intellicare.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AIProperties {

    private boolean mockEnabled = true;

    private OpenAI openai = new OpenAI();
    private Whisper whisper = new Whisper();
    private ElevenLabs elevenlabs = new ElevenLabs();

    @Getter
    @Setter
    public static class OpenAI {
        private String apiKey;
        private String model = "gpt-4o";
        private double temperature = 0.7;
        private int maxTokens = 2048;
    }

    @Getter
    @Setter
    public static class Whisper {
        private String model = "whisper-1";
    }

    @Getter
    @Setter
    public static class ElevenLabs {
        private String apiKey;
        private String voiceId = "21m00Tcm4TlvDq8ikWAM";
    }
}
