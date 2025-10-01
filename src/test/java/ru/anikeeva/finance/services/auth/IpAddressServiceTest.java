package ru.anikeeva.finance.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IpAddressServiceTest {
    @InjectMocks
    private IpAddressService ipAddressService;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("Получение ip по заголовку")
    public void getClientIpByHeader() {
        String expectedIp = "192.168.1.1";

        when(request.getHeader("X-Forwarded-For")).thenReturn(expectedIp);
        String actualIp = ipAddressService.getClientIP(request);

        assertEquals(expectedIp, actualIp);
    }

    @Test
    @DisplayName(("Получение ip при пустом заголовке запроса"))
    public void getClientIpWithEmptyHeader() {
        String expectedIp = "192.168.1.1";

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(expectedIp);
        String actualIp = ipAddressService.getClientIP(request);

        assertEquals(expectedIp, actualIp);
    }
}