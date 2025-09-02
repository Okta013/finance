package ru.anikeeva.finance.services.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.notifications.BudgetNotification;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyJobCompletion(UUID userId, String message) {
        String destination = "/topic/jobs/" + userId;
        log.info("Пользователю {} отправляется уведомление: {}", userId, message);
        messagingTemplate.convertAndSend(destination, message);
    }

    public void sendBudgetWarning(String userId, BudgetNotification notification) {
        log.info("Пользователю {} отправляется уведомление о превышении бюджета", userId);
        messagingTemplate.convertAndSend("/topic/budget-alerts/" + userId, notification);
    }
}