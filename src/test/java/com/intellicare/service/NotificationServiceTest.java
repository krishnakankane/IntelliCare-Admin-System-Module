package com.intellicare.service;

import com.intellicare.dto.request.NotificationRequest;
import com.intellicare.dto.response.NotificationResponse;
import com.intellicare.entity.Notification;
import com.intellicare.entity.User;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.repository.NotificationRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;
    @Mock private WhatsAppService whatsAppService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).email("patient@test.com").phoneNumber("+919999999999")
                .firstName("Patient").lastName("One").isActive(true).build();

        testNotification = Notification.builder()
                .id(1L).user(testUser).title("Test").message("Body")
                .channel(Notification.Channel.IN_APP)
                .status(Notification.Status.SENT).isRead(false)
                .createdAt(Instant.now()).build();
    }

    @Test
    @DisplayName("send — persists notification and returns response")
    void send_persistsAndReturns() {
        NotificationRequest.Send request = new NotificationRequest.Send();
        request.setUserId(1L);
        request.setTitle("Hello");
        request.setMessage("World");
        request.setChannel(Notification.Channel.IN_APP);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any())).thenReturn(testNotification);

        NotificationResponse response = notificationService.send(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("send — unknown user throws ResourceNotFoundException")
    void send_unknownUser_throws() {
        NotificationRequest.Send request = new NotificationRequest.Send();
        request.setUserId(999L);
        request.setTitle("X");
        request.setMessage("Y");
        request.setChannel(Notification.Channel.EMAIL);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.send(request, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("countUnread — delegates to repository")
    void countUnread_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);
        assertThat(notificationService.countUnread(1L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("markAllAsRead — returns number updated")
    void markAllAsRead_returnsCount() {
        when(notificationRepository.markAllAsRead(eq(1L), any(Instant.class))).thenReturn(3);
        assertThat(notificationService.markAllAsRead(1L)).isEqualTo(3);
    }
}
