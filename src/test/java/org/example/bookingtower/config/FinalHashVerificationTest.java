package org.example.bookingtower.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class FinalHashVerificationTest {

    @Test
    public void generateAndVerifyCorrectHash() {
        System.out.println("[DEBUG_LOG] Generating and verifying correct hash for 'password'");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Генерируем новый хеш прямо в тесте
        String generatedHash = encoder.encode("password");
        System.out.println("[DEBUG_LOG] Generated hash: " + generatedHash);

        // Сразу проверяем, что он работает
        boolean correctPasswordMatches = encoder.matches("password", generatedHash);
        System.out.println("[DEBUG_LOG] Generated hash matches 'password': " + correctPasswordMatches);

        // Проверяем неправильные пароли
        boolean wrongPassword1 = encoder.matches("admin", generatedHash);
        boolean wrongPassword2 = encoder.matches("123456", generatedHash);
        boolean wrongPassword3 = encoder.matches("wrongpassword", generatedHash);

        System.out.println("[DEBUG_LOG] Generated hash matches 'admin': " + wrongPassword1);
        System.out.println("[DEBUG_LOG] Generated hash matches '123456': " + wrongPassword2);
        System.out.println("[DEBUG_LOG] Generated hash matches 'wrongpassword': " + wrongPassword3);

        // Утверждения
        assertTrue(correctPasswordMatches, "Generated hash should match 'password'");
        assertFalse(wrongPassword1, "Generated hash should NOT match 'admin'");
        assertFalse(wrongPassword2, "Generated hash should NOT match '123456'");
        assertFalse(wrongPassword3, "Generated hash should NOT match 'wrongpassword'");

        System.out.println("[DEBUG_LOG] ✅ Hash generation and verification PASSED!");
        System.out.println("[DEBUG_LOG] ===== USE THIS HASH IN MIGRATION =====");
        System.out.println("[DEBUG_LOG] " + generatedHash);
        System.out.println("[DEBUG_LOG] =====================================");
    }
}
