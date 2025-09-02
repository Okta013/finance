package ru.anikeeva.finance.services.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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
}