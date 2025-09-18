package org.example.bookingtower.config;

import org.example.bookingtower.domain.entity.User;
import org.example.bookingtower.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SecurityConfigTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Test
    public void testPasswordEncoderBean() {
        System.out.println("[DEBUG_LOG] Testing PasswordEncoder bean");
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder instanceof org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder);

        String rawPassword = "password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        System.out.println("[DEBUG_LOG] PasswordEncoder works correctly");
    }

    @Test
    public void testAuthenticationManagerBean() {
        System.out.println("[DEBUG_LOG] Testing AuthenticationManager bean");
        assertNotNull(authenticationManager);
        System.out.println("[DEBUG_LOG] AuthenticationManager bean exists");
    }

    @Test
    public void testUserDetailsServiceBean() {
        System.out.println("[DEBUG_LOG] Testing CustomUserDetailsService bean");
        assertNotNull(userDetailsService);
        System.out.println("[DEBUG_LOG] CustomUserDetailsService bean exists");
    }

    @Test
    public void testAdminAuthentication() {
        System.out.println("[DEBUG_LOG] Testing admin authentication");

        // Проверяем, что админ существует в базе данных
        Optional<User> adminUser = userRepository.findByEmail("admin@bookingtower.com");
        assertTrue(adminUser.isPresent(), "Admin user should exist in database");

        User admin = adminUser.get();
        System.out.println("[DEBUG_LOG] Admin user found: " + admin.getEmail());
        System.out.println("[DEBUG_LOG] Admin password hash: " + admin.getPasswordHash());

        // Ожидаемый правильный хеш для пароля "password"
        String expectedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye7Iy/Vo1V4FvUV/TxwYl4LaY.HCfi9/u";
        System.out.println("[DEBUG_LOG] Expected hash: " + expectedHash);
        System.out.println("[DEBUG_LOG] Admin role: " + admin.getRole());
        System.out.println("[DEBUG_LOG] Admin email verified: " + admin.getEmailVerified());

        // Проверяем, что хеш пароля соответствует паролю "password"
        boolean passwordMatches = passwordEncoder.matches("password", admin.getPasswordHash());
        assertTrue(passwordMatches, "Password should match the hash");
        System.out.println("[DEBUG_LOG] Password matches hash: " + passwordMatches);

        // Тестируем аутентификацию через AuthenticationManager
        try {
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken("admin@bookingtower.com", "password");

            Authentication result = authenticationManager.authenticate(authToken);

            assertNotNull(result);
            assertTrue(result.isAuthenticated());
            assertEquals("admin@bookingtower.com", result.getName());
            assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

            System.out.println("[DEBUG_LOG] Authentication successful!");
            System.out.println("[DEBUG_LOG] Authenticated user: " + result.getName());
            System.out.println("[DEBUG_LOG] Authorities: " + result.getAuthorities());

        } catch (AuthenticationException e) {
            fail("Authentication should succeed for admin user: " + e.getMessage());
        }
    }

    @Test
    public void testWrongPasswordAuthentication() {
        System.out.println("[DEBUG_LOG] Testing authentication with wrong password");

        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken("admin@bookingtower.com", "wrongpassword");

        assertThrows(AuthenticationException.class, () -> {
            authenticationManager.authenticate(authToken);
        });

        System.out.println("[DEBUG_LOG] Authentication correctly failed for wrong password");
    }

    @Test
    public void testNonExistentUserAuthentication() {
        System.out.println("[DEBUG_LOG] Testing authentication with non-existent user");

        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken("nonexistent@example.com", "password");

        assertThrows(AuthenticationException.class, () -> {
            authenticationManager.authenticate(authToken);
        });

        System.out.println("[DEBUG_LOG] Authentication correctly failed for non-existent user");
    }
}
