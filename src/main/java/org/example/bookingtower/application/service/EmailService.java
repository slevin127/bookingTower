package org.example.bookingtower.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:127slevin@gmail.com}")
    private String fromEmail;

    @Value("${server.port:8080}")
    private String serverPort;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmailVerification(String toEmail, String verificationToken) {
        logger.info("Sending email verification to: {}", toEmail);

        String subject = "Подтвердите ваш email - BookingTower";
        String verificationUrl = "http://localhost:" + serverPort + "/verify-email?token=" + verificationToken;

        String text = String.format(
            "Добро пожаловать в BookingTower!\n\n" +
            "Для завершения регистрации, пожалуйста, подтвердите ваш email адрес, перейдя по ссылке:\n\n" +
            "%s\n\n" +
            "Ссылка действительна в течение 24 часов.\n\n" +
            "Если вы не регистрировались на нашем сайте, просто проигнорируйте это письмо.\n\n" +
            "С уважением,\n" +
            "Команда BookingTower",
            verificationUrl
        );

        sendEmail(toEmail, subject, text);
    }

    public void sendPasswordReset(String toEmail, String resetToken) {
        logger.info("Sending password reset email to: {}", toEmail);

        String subject = "Сброс пароля - BookingTower";
        String resetUrl = "http://localhost:" + serverPort + "/auth/reset-password?token=" + resetToken;

        String text = String.format(
            "Здравствуйте!\n\n" +
            "Вы запросили сброс пароля для вашего аккаунта в BookingTower.\n\n" +
            "Для создания нового пароля перейдите по ссылке:\n\n" +
            "%s\n\n" +
            "Ссылка действительна в течение 2 часов.\n\n" +
            "Если вы не запрашивали сброс пароля, просто проигнорируйте это письмо.\n\n" +
            "С уважением,\n" +
            "Команда BookingTower",
            resetUrl
        );

        sendEmail(toEmail, subject, text);
    }

    public void sendBookingConfirmation(String toEmail, String bookingDetails) {
        logger.info("Sending booking confirmation to: {}", toEmail);

        String subject = "Подтверждение бронирования - BookingTower";

        String text = String.format(
            "Здравствуйте!\n\n" +
            "Ваше бронирование успешно подтверждено.\n\n" +
            "Детали бронирования:\n%s\n\n" +
            "Спасибо за использование BookingTower!\n\n" +
            "С уважением,\n" +
            "Команда BookingTower",
            bookingDetails
        );

        sendEmail(toEmail, subject, text);
    }

    public void sendBookingCancellation(String toEmail, String bookingDetails, String reason) {
        logger.info("Sending booking cancellation to: {}", toEmail);

        String subject = "Отмена бронирования - BookingTower";

        String text = String.format(
            "Здравствуйте!\n\n" +
            "Ваше бронирование было отменено.\n\n" +
            "Детали бронирования:\n%s\n\n" +
            "Причина отмены: %s\n\n" +
            "Если у вас есть вопросы, свяжитесь с нами.\n\n" +
            "С уважением,\n" +
            "Команда BookingTower",
            bookingDetails,
            reason != null ? reason : "Не указана"
        );

        sendEmail(toEmail, subject, text);
    }

    public void sendBookingReminder(String toEmail, String bookingDetails) {
        logger.info("Sending booking reminder to: {}", toEmail);

        String subject = "Напоминание о бронировании - BookingTower";

        String text = String.format(
            "Здравствуйте!\n\n" +
            "Напоминаем о вашем предстоящем бронировании:\n\n" +
            "%s\n\n" +
            "Не забудьте прийти вовремя!\n\n" +
            "С уважением,\n" +
            "Команда BookingTower",
            bookingDetails
        );

        sendEmail(toEmail, subject, text);
    }

    public void sendPaymentConfirmation(String toEmail, String paymentDetails) {
        logger.info("Sending payment confirmation to: {}", toEmail);

        String subject = "Подтверждение оплаты - BookingTower";

        String text = String.format(
            "Здравствуйте!\n\n" +
            "Ваш платеж успешно обработан.\n\n" +
            "Детали платежа:\n%s\n\n" +
            "Спасибо за использование BookingTower!\n\n" +
            "С уважением,\n" +
            "Команда BookingTower",
            paymentDetails
        );

        sendEmail(toEmail, subject, text);
    }

    private void sendEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);

            logger.info("Attempting to send email to: {} with subject: {}", toEmail, subject);
            logger.debug("Email from: {}", fromEmail);

            mailSender.send(message);
            logger.info("Email sent successfully to: {}", toEmail);

        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Check email credentials: {}", e.getMessage());
            throw new RuntimeException("Email authentication failed. Please check email configuration.", e);
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email due to mail server issues: {}", e.getMessage());
            throw new RuntimeException("Failed to send email due to server issues.", e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
