package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.SendNotificationRequest;
import com.example.demo.enums.NotificationStatus;
import com.example.demo.enums.NotificationType;
import com.example.demo.entity.Notification;
import com.example.demo.entity.NotificationTemplate;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.NotificationTemplateRepository;
import com.example.demo.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final JavaMailSender mailSender;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
            NotificationTemplateRepository templateRepository,
            @Autowired(required = false) JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional
    public Notification sendNotification(SendNotificationRequest request) {
        NotificationTemplate template = templateRepository
                .findByTemplateCodeAndIsActiveTrue(request.getTemplateCode())
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + request.getTemplateCode()));

        String subject = applyParams(template.getSubjectTemplate(), request.getParams());
        String body = applyParams(template.getBodyTemplate(), request.getParams());

        Notification notification = new Notification();
        notification.setRecipient(request.getRecipient());
        notification.setType(NotificationType.EMAIL);
        notification.setSubject(subject);
        notification.setContent(body);
        notification.setRelatedEntityId(request.getRelatedEntityId());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);

        notification = notificationRepository.save(notification);


        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(request.getRecipient());
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception ex) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(ex.getMessage());
                notification.setRetryCount(notification.getRetryCount() + 1);
            }
        } else {

            System.out.println("Mail sender not configured, notification saved as PENDING: " + notification.getId());
        }

        return notificationRepository.save(notification);
    }

    @Override
    public java.util.List<Notification> getNotificationsForRecipient(String recipient) {
        return notificationRepository.findByRecipient(recipient);
    }

    private String applyParams(String template, Map<String, Object> params) {
        if (params == null || params.isEmpty() || template == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
