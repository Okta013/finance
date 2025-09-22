package ru.anikeeva.finance.controllers.mail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.mail.MailService;

@RestController
@RequestMapping("/api/v1/mails")
@RequiredArgsConstructor
@Tag(name = "Контроллер управления почтой")
public class MailController {
    private final MailService mailService;

    @PatchMapping("/email/confirm")
    @Operation(summary = "Подтверждение электронной почты",
        description = "Отправляет письмо с подтверждением адреса электронной почты для последующих рассылок")
    public ResponseEntity<Void> sendEmail(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        mailService.sendEmail(currentUser);
        return ResponseEntity.noContent().build();
    }
}