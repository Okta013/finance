package ru.anikeeva.finance.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import ru.anikeeva.finance.services.auth.IpAddressService;
import ru.anikeeva.finance.services.auth.LoginAttemptService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final LoginAttemptService loginAttemptService;
    private final IpAddressService ipAddressService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String ip = ipAddressService.getClientIP(request);
        if (username != null && ip != null) {
            loginAttemptService.loginFailed(username, ip);
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Аутентификация не выполнена" + exception.getMessage());
    }
}