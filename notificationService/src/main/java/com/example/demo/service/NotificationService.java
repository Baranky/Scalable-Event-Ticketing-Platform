package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.SendNotificationRequest;
import com.example.demo.model.Notification;

@Service
public interface NotificationService {


    Notification sendNotification(SendNotificationRequest request);

    List<Notification> getNotificationsForRecipient(String recipient);
}
