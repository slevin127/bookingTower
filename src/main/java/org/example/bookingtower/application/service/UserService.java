package org.example.bookingtower.application.service;

import org.example.bookingtower.domain.entity.User;
import org.example.bookingtower.infrastructure.repository.UserRepository;
import org.example.bookingtower.web.controller.RegistrationController.RegistrationForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User registerUser(String email, String password) {
        logger.info("Registering new user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.USER);
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(user);

        // Send verification email
        try {
            emailService.sendEmailVerification(savedUser.getEmail(), verificationToken);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", email, e);
            // Don't fail registration if email sending fails
        }

        logger.info("User registered successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    public User registerUserWithProfile(String email, String password, User.UserType userType, RegistrationForm form) {
        logger.info("Registering new user with profile - email: {}, userType: {}", email, userType);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.USER);
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUserType(userType);

        // Set profile fields based on user type
        if (userType == User.UserType.INDIVIDUAL) {
            user.setFirstName(form.getFirstName());
            user.setLastName(form.getLastName());
            user.setMiddleName(form.getMiddleName());
            user.setPhone(form.getPhone());
        } else if (userType == User.UserType.LEGAL_ENTITY) {
            user.setCompanyName(form.getCompanyName());
            user.setInn(form.getInn());
            user.setOgrn(form.getOgrn());
            user.setKpp(form.getKpp());
            user.setLegalAddress(form.getLegalAddress());
            user.setActualAddress(form.getActualAddress());
            user.setDirectorName(form.getDirectorName());
        }

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(user);

        // Send verification email
        try {
            emailService.sendEmailVerification(savedUser.getEmail(), verificationToken);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", email, e);
            // Don't fail registration if email sending fails
        }

        logger.info("User with profile registered successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    public boolean verifyEmail(String token) {
        logger.info("Verifying email with token: {}", token);

        Optional<User> userOpt = userRepository.findByValidEmailVerificationToken(token, LocalDateTime.now());
        if (userOpt.isEmpty()) {
            logger.warn("Invalid or expired email verification token: {}", token);
            return false;
        }

        User user = userOpt.get();
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);

        userRepository.save(user);
        logger.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }

    public void initiatePasswordReset(String email) {
        logger.info("Initiating password reset for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.warn("Password reset requested for non-existent email: {}", email);
            // Don't reveal that email doesn't exist
            return;
        }

        User user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(2));

        userRepository.save(user);

        try {
            emailService.sendPasswordReset(user.getEmail(), resetToken);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", email, e);
            throw new RuntimeException("Failed to send password reset email");
        }

        logger.info("Password reset initiated for user: {}", email);
    }

    public boolean resetPassword(String token, String newPassword) {
        logger.info("Resetting password with token: {}", token);

        Optional<User> userOpt = userRepository.findByValidPasswordResetToken(token, LocalDateTime.now());
        if (userOpt.isEmpty()) {
            logger.warn("Invalid or expired password reset token: {}", token);
            return false;
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);

        userRepository.save(user);
        logger.info("Password reset successfully for user: {}", user.getEmail());
        return true;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean validatePassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        logger.info("Changing password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!validatePassword(user, currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password changed successfully for user ID: {}", userId);
    }

    public List<User> findUnverifiedUsersOlderThan(LocalDateTime cutoffDate) {
        return userRepository.findUnverifiedUsersOlderThan(cutoffDate);
    }

    public void deleteUser(Long userId) {
        logger.info("Deleting user with ID: {}", userId);
        userRepository.deleteById(userId);
    }

    public long countAdminUsers() {
        return userRepository.countAdminUsers();
    }

    public User promoteToAdmin(Long userId) {
        logger.info("Promoting user to admin: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(User.Role.ADMIN);
        return userRepository.save(user);
    }

    public void resendVerificationEmail(String email) {
        logger.info("Resending verification email to: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || userOpt.get().getEmailVerified()) {
            return; // Don't reveal user existence or verification status
        }

        User user = userOpt.get();
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        try {
            emailService.sendEmailVerification(user.getEmail(), verificationToken);
        } catch (Exception e) {
            logger.error("Failed to resend verification email to {}", email, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }
}
