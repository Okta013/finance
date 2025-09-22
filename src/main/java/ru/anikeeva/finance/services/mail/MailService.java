package ru.anikeeva.finance.services.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.IntegrationException;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final MessageSource messages;
    private final JavaMailSender mailSender;
    private final UserService userService;
    private final VerificationTokenService verificationTokenService;

    @Value("${spring.mail.url_for_confirm}")
    private String confirmUrl;

    @Value("${spring.mail.username}")
    private String from;

    private static final String APP_URL = "/api/v1/emails/confirm?token=";

    public void sendEmail (final UserDetailsImpl currentUser) {
        User user = userService.findUserById(currentUser.getId());
        try {
            String confirmationUrl = confirmUrl + APP_URL + verificationTokenService.generateVerificationToken(user);
            String message = messages.getMessage("message.confirm", null, Locale.getDefault());
            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(user.getEmail());
            helper.setSubject("Подтверждение адреса электронной почты");

            String htmlContent =
                "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "</head>\n" +
                    "<body style=\"font-family: Arial, sans-serif; line-height: 1.6;\">\n" +
                    "    <div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">\n" +
                    "        <p>" + message + "</p>\n" +
                    "        <p style=\"margin: 20px 0;\">\n" +
                    "            <a href=\"" + confirmationUrl + "\" style=\"display: inline-block; background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Подтвердить регистрацию</a>\n" +
                    "        </p>\n" +
                    "        <p>Если кнопка не работает, скопируйте следующую ссылку в адресную строку браузера:</p>\n" +
                    "        <p><a href=\"" + confirmationUrl + "\">" + confirmationUrl + "</a></p>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(mailMessage);
        } catch (MessagingException e) {
            throw new IntegrationException("Произошла ошибка при попытке отправки письма с подтверждением на указанный email");
        }
    }
}