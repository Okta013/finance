package ru.anikeeva.finance.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.anikeeva.finance.services.auth.IpAddressService;
import ru.anikeeva.finance.services.auth.LoginAttemptService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final LoginAttemptService loginAttemptService;
    private final IpAddressService ipAddressService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String username = authentication.getName();
        String ip = ipAddressService.getClientIP(request);
        loginAttemptService.loginSucceeded(username, ip);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}