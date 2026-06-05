package com.intellicare.repository;

import com.intellicare.entity.Notification;
import com.intellicare.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByName(String name);

    Optional<NotificationTemplate> findByNameAndIsActiveTrue(String name);

    List<NotificationTemplate> findByChannelAndIsActiveTrue(Notification.Channel channel);
}
