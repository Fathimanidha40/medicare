package com.medicare;

import com.medicare.dto.AuthResponse;
import com.medicare.dto.LoginRequest;
import com.medicare.dto.RegisterRequest;
import com.medicare.exception.BadRequestException;
import com.medicare.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testRegisterAndLogin() {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("John Tester");
        reg.setEmail("john.test@example.com");
        reg.setPassword("Password123!");
        reg.setPhone("+15551234567");
        reg.setDateOfBirth(LocalDate.of(1990, 5, 15));

        AuthResponse regResp = authService.register(reg);
        Assertions.assertNotNull(regResp);
        Assertions.assertNotNull(regResp.getToken());
        Assertions.assertEquals("john.test@example.com", regResp.getEmail());

        // Test login
        LoginRequest loginReq = new LoginRequest("john.test@example.com", "Password123!");
        AuthResponse loginResp = authService.login(loginReq);
        Assertions.assertNotNull(loginResp);
        Assertions.assertNotNull(loginResp.getToken());

        // Test duplicate registration throws BadRequestException
        Assertions.assertThrows(BadRequestException.class, () -> authService.register(reg));
    }
}
