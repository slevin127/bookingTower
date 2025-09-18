package org.example.bookingtower.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class SimplePasswordTest {

    @Test
    public void testPasswordHashCorrectness() {
        System.out.println("[DEBUG_LOG] Testing password hash correctness");
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Старый неправильный хеш
        String oldHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.";
        
        // Новый правильный хеш для пароля "password"
        String newHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye7Iy/Vo1V4FvUV/TxwYl4LaY.HCfi9/u";
        
        System.out.println("[DEBUG_LOG] Testing old hash: " + oldHash);
        boolean oldMatches = encoder.matches("password", oldHash);
        System.out.println("[DEBUG_LOG] Old hash matches 'password': " + oldMatches);
        
        System.out.println("[DEBUG_LOG] Testing new hash: " + newHash);
        boolean newMatches = encoder.matches("password", newHash);
        System.out.println("[DEBUG_LOG] New hash matches 'password': " + newMatches);
        
        // Проверяем, что новый хеш правильный
        assertTrue(newMatches, "New hash should match password 'password'");
        
        // Проверяем, что старый хеш неправильный
        assertFalse(oldMatches, "Old hash should NOT match password 'password'");
        
        System.out.println("[DEBUG_LOG] Password hash test completed successfully!");
        System.out.println("[DEBUG_LOG] The new hash in database migration is correct for password 'password'");
    }
    
    @Test
    public void testGenerateNewHash() {
        System.out.println("[DEBUG_LOG] Generating fresh hash for 'password'");
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String freshHash = encoder.encode("password");
        
        System.out.println("[DEBUG_LOG] Fresh generated hash: " + freshHash);
        
        boolean matches = encoder.matches("password", freshHash);
        System.out.println("[DEBUG_LOG] Fresh hash matches 'password': " + matches);
        
        assertTrue(matches, "Fresh generated hash should match 'password'");
    }
}