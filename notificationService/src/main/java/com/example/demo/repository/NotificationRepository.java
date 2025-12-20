package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.enums.NotificationStatus;
import com.example.demo.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipient(String recipient);

    List<Notification> findByStatus(NotificationStatus status);
}
