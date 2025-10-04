package org.example.bookingtower.web.controller;

import org.example.bookingtower.application.service.UserService;
import org.example.bookingtower.domain.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Веб-контроллер RegistrationController для страниц приложения BookingTower.
 */
@Controller
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    private final UserService userService;

    @Autowired
    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        
        logger.info("Processing registration for email: {}, userType: {}", form.getEmail(), form.getUserType());

        // Проверяем корректность формы
        if (bindingResult.hasErrors()) {
            logger.warn("Registration form has validation errors: {}", bindingResult.getAllErrors());
            return "register";
        }

        // Проверяем совпадение пароля и подтверждения
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "Пароли не совпадают");
            return "register";
        }

        // Проверяем обязательные поля в зависимости от типа пользователя
        if (form.getUserType() == User.UserType.INDIVIDUAL) {
            if (form.getFirstName() == null || form.getFirstName().trim().isEmpty() ||
                form.getLastName() == null || form.getLastName().trim().isEmpty()) {
                model.addAttribute("error", "Для физических лиц обязательны поля: Имя и Фамилия");
                return "register";
            }
        } else if (form.getUserType() == User.UserType.LEGAL_ENTITY) {
            if (form.getCompanyName() == null || form.getCompanyName().trim().isEmpty() ||
                form.getInn() == null || form.getInn().trim().isEmpty()) {
                model.addAttribute("error", "Для юридических лиц обязательны поля: Название организации и ИНН");
                return "register";
            }
            
            // Проверяем формат ИНН
            String cleanInn = form.getInn().replaceAll("\\D", "");
            if (cleanInn.length() != 10 && cleanInn.length() != 12) {
                model.addAttribute("error", "ИНН должен содержать 10 или 12 цифр");
                return "register";
            }
        }

        try {
            // Создаём пользователя
            User user = userService.registerUserWithProfile(
                form.getEmail(),
                form.getPassword(),
                form.getUserType(),
                form
            );

            logger.info("User registered successfully with ID: {}", user.getId());
            
            redirectAttributes.addFlashAttribute("success", 
                "Регистрация успешна! Проверьте email для подтверждения аккаунта.");
            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "register";
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            model.addAttribute("error", "Произошла ошибка при регистрации. Попробуйте позже.");
            return "register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Email verification requested with token: {}", token);

        try {
            boolean verified = userService.verifyEmail(token);
            
            if (verified) {
                redirectAttributes.addFlashAttribute("success", "Email СѓСЃРїРµС€РЅРѕ РїРѕРґС‚РІРµСЂР¶РґРµРЅ! РўРµРїРµСЂСЊ РІС‹ РјРѕР¶РµС‚Рµ РІРѕР№С‚Рё РІ СЃРёСЃС‚РµРјСѓ.");
                return "redirect:/login";
            } else {
                model.addAttribute("error", "Недействительная или истекшая ссылка подтверждения");
                return "login";
            }
        } catch (Exception e) {
            logger.error("Error during email verification", e);
            model.addAttribute("error", "Произошла ошибка при подтверждении email");
            return "login";
        }
    }

    @GetMapping("/resend-verification")
    public String showResendVerificationForm() {
        return "resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email, 
                                   Model model, 
                                   RedirectAttributes redirectAttributes) {
        logger.info("Resend verification requested for email: {}", email);

        try {
            userService.resendVerificationEmail(email);
            redirectAttributes.addFlashAttribute("success", 
                "Если аккаунт существует и не подтвержден, письмо с подтверждением было отправлено.");
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("Error resending verification email", e);
            model.addAttribute("error", "Произошла ошибка при отправке письма");
            return "resend-verification";
        }
    }

    // DTO для формы регистрации
    public static class RegistrationForm {
        private String email;
        private String password;
        private String confirmPassword;
        private User.UserType userType = User.UserType.INDIVIDUAL;
        
        // Поля для физического лица
        private String firstName;
        private String lastName;
        private String middleName;
        private String phone;
        
        // Поля для юридического лица
        private String companyName;
        private String inn;
        private String ogrn;
        private String kpp;
        private String legalAddress;
        private String actualAddress;
        private String directorName;

        // Конструкторы
        public RegistrationForm() {}

        // Геттеры и сеттеры
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }

        public User.UserType getUserType() {
            return userType;
        }

        public void setUserType(User.UserType userType) {
            this.userType = userType;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getMiddleName() {
            return middleName;
        }

        public void setMiddleName(String middleName) {
            this.middleName = middleName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getInn() {
            return inn;
        }

        public void setInn(String inn) {
            this.inn = inn;
        }

        public String getOgrn() {
            return ogrn;
        }

        public void setOgrn(String ogrn) {
            this.ogrn = ogrn;
        }

        public String getKpp() {
            return kpp;
        }

        public void setKpp(String kpp) {
            this.kpp = kpp;
        }

        public String getLegalAddress() {
            return legalAddress;
        }

        public void setLegalAddress(String legalAddress) {
            this.legalAddress = legalAddress;
        }

        public String getActualAddress() {
            return actualAddress;
        }

        public void setActualAddress(String actualAddress) {
            this.actualAddress = actualAddress;
        }

        public String getDirectorName() {
            return directorName;
        }

        public void setDirectorName(String directorName) {
            this.directorName = directorName;
        }
    }
}

