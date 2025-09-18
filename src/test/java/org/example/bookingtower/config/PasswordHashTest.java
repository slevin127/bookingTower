package org.example.bookingtower.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {

    @Test
    public void testPasswordHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String existingHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.";
        
        System.out.println("[DEBUG_LOG] Testing existing hash against different passwords:");
        
        // Тестируем разные возможные пароли
        String[] possiblePasswords = {"password", "admin", "123456", "secret", "test"};
        
        for (String pwd : possiblePasswords) {
            boolean matches = encoder.matches(pwd, existingHash);
            System.out.println("[DEBUG_LOG] Password '" + pwd + "' matches existing hash: " + matches);
        }
        
        System.out.println("\n[DEBUG_LOG] Generating new hash for 'password':");
        String newHash = encoder.encode("password");
        System.out.println("[DEBUG_LOG] New hash for 'password': " + newHash);
        
        boolean newHashMatches = encoder.matches("password", newHash);
        System.out.println("[DEBUG_LOG] New hash matches 'password': " + newHashMatches);
        
        System.out.println("\n[DEBUG_LOG] Testing if existing hash is valid BCrypt:");
        try {
            // Попробуем проверить существующий хеш с любым паролем
            encoder.matches("test", existingHash);
            System.out.println("[DEBUG_LOG] Existing hash is valid BCrypt format");
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Existing hash is NOT valid BCrypt format: " + e.getMessage());
        }
    }
}