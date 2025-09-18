package org.example.bookingtower.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class GenerateCorrectHashTest {

    @Test
    public void generateCorrectHashForPassword() {
        System.out.println("[DEBUG_LOG] Generating correct BCrypt hash for 'password'");
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Генерируем несколько хешей для пароля "password"
        for (int i = 1; i <= 3; i++) {
            String hash = encoder.encode("password");
            boolean matches = encoder.matches("password", hash);
            
            System.out.println("[DEBUG_LOG] Hash " + i + ": " + hash);
            System.out.println("[DEBUG_LOG] Hash " + i + " matches 'password': " + matches);
            
            assertTrue(matches, "Generated hash should match 'password'");
        }
        
        // Используем первый сгенерированный хеш для обновления миграции
        String correctHash = encoder.encode("password");
        System.out.println("\n[DEBUG_LOG] ===== CORRECT HASH TO USE IN MIGRATION =====");
        System.out.println("[DEBUG_LOG] " + correctHash);
        System.out.println("[DEBUG_LOG] ============================================");
        
        // Проверяем, что этот хеш действительно работает
        assertTrue(encoder.matches("password", correctHash), "Final hash should match 'password'");
        assertFalse(encoder.matches("wrongpassword", correctHash), "Final hash should NOT match wrong password");
        
        System.out.println("[DEBUG_LOG] Hash verification completed successfully!");
    }
}