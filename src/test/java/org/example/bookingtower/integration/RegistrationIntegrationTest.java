package org.example.bookingtower.integration;

import org.example.bookingtower.application.service.UserService;
import org.example.bookingtower.domain.entity.User;
import org.example.bookingtower.infrastructure.repository.UserRepository;
import org.example.bookingtower.web.controller.RegistrationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.mail.host=smtp.gmail.com",
    "spring.mail.port=587",
    "spring.mail.username=test@example.com",
    "spring.mail.password=testpassword",
    "spring.mail.properties.mail.smtp.auth=true",
    "spring.mail.properties.mail.smtp.starttls.enable=true",
    "app.mail.from=test@example.com"
})
@Transactional
public class RegistrationIntegrationTest {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testUserRegistrationWithProfile() {
        System.out.println("[DEBUG_LOG] Testing user registration with profile...");
        
        // Create registration form
        RegistrationController.RegistrationForm form = new RegistrationController.RegistrationForm();
        form.setEmail("test@example.com");
        form.setPassword("testpassword123");
        form.setConfirmPassword("testpassword123");
        form.setUserType(User.UserType.INDIVIDUAL);
        form.setFirstName("Test");
        form.setLastName("User");
        form.setPhone("+1234567890");
        
        try {
            // Register user
            User registeredUser = userService.registerUserWithProfile(
                form.getEmail(),
                form.getPassword(),
                form.getUserType(),
                form
            );
            
            System.out.println("[DEBUG_LOG] User registered successfully with ID: " + registeredUser.getId());
            
            // Verify user was created
            assertNotNull(registeredUser);
            assertNotNull(registeredUser.getId());
            assertEquals("test@example.com", registeredUser.getEmail());
            assertEquals(User.UserType.INDIVIDUAL, registeredUser.getUserType());
            assertEquals("Test", registeredUser.getFirstName());
            assertEquals("User", registeredUser.getLastName());
            assertFalse(registeredUser.getEmailVerified());
            assertNotNull(registeredUser.getEmailVerificationToken());
            
            System.out.println("[DEBUG_LOG] User registration test completed successfully!");
            System.out.println("[DEBUG_LOG] Verification token: " + registeredUser.getEmailVerificationToken());
            
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] User registration test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Registration should not fail: " + e.getMessage());
        }
    }
    
    @Test
    public void testEmailVerification() {
        System.out.println("[DEBUG_LOG] Testing email verification...");
        
        // First register a user
        RegistrationController.RegistrationForm form = new RegistrationController.RegistrationForm();
        form.setEmail("verify@example.com");
        form.setPassword("testpassword123");
        form.setUserType(User.UserType.INDIVIDUAL);
        form.setFirstName("Verify");
        form.setLastName("User");
        
        try {
            User registeredUser = userService.registerUserWithProfile(
                form.getEmail(),
                form.getPassword(),
                form.getUserType(),
                form
            );
            
            String verificationToken = registeredUser.getEmailVerificationToken();
            assertNotNull(verificationToken);
            
            // Verify email
            boolean verified = userService.verifyEmail(verificationToken);
            assertTrue(verified);
            
            // Check that user is now verified
            User verifiedUser = userRepository.findById(registeredUser.getId()).orElse(null);
            assertNotNull(verifiedUser);
            assertTrue(verifiedUser.getEmailVerified());
            assertNull(verifiedUser.getEmailVerificationToken());
            
            System.out.println("[DEBUG_LOG] Email verification test completed successfully!");
            
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Email verification test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Email verification should not fail: " + e.getMessage());
        }
    }
}