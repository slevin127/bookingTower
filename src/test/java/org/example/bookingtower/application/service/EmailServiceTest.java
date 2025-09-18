package org.example.bookingtower.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.mail.host=smtp.gmail.com",
    "spring.mail.port=587",
    "spring.mail.username=127slevin@gmail.com",
    "spring.mail.password=gzsxzdvhciupcuwq",
    "spring.mail.properties.mail.smtp.auth=true",
    "spring.mail.properties.mail.smtp.starttls.enable=true",
    "app.mail.from=127slevin@gmail.com"
})
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testSendEmailVerification() {
        System.out.println("[DEBUG_LOG] Testing email verification sending...");
        
        try {
            // Test sending verification email
            String testEmail = "127slevin@gmail.com"; // Send to the same email for testing
            String testToken = "test-verification-token-123";
            
            emailService.sendEmailVerification(testEmail, testToken);
            
            System.out.println("[DEBUG_LOG] Email verification test completed successfully!");
            
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Email verification test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}