package ru.anikeeva.finance.controllers.mail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.anikeeva.finance.dto.mail.ConfirmEmailResponse;
import ru.anikeeva.finance.services.mail.VerificationTokenService;

@Controller
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
@Tag(name = "Контроллер управления подтверждением email")
public class ConfirmEmailController {
    private final VerificationTokenService verificationTokenService;

    @Operation(summary = "Подтверждение email",
        description = "Подтверждает адрес электронной почты с указанным токеном и отображает страницу результата")
    @GetMapping("/confirm")
    public String confirmEmail(@RequestParam("token") String token, Model model) {
        try {
            ConfirmEmailResponse response = verificationTokenService.confirmEmail(token);
            if (response.isSuccess()) {
                return "email-confirmation-success";
            } else {
                model.addAttribute("errorMessage", response.message());
                return "email-confirmation-error";
            }
        } catch (Exception e){
            model.addAttribute("errorMessage", "Произошла непредвиденная ошибка!");
            return "email-confirmation-error";
        }
    }
}