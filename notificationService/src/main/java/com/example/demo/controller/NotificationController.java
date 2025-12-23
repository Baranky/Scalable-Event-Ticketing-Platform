package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.SendNotificationRequest;
import com.example.demo.entity.Notification;
import com.example.demo.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Notification> send(@RequestBody SendNotificationRequest request) {
        Notification notification = notificationService.sendNotification(request);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/recipient/{recipient}")
    public ResponseEntity<List<Notification>> getByRecipient(@PathVariable String recipient) {
        List<Notification> notifications = notificationService.getNotificationsForRecipient(recipient);
        return ResponseEntity.ok(notifications);
    }
}
