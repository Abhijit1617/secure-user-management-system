package com.controlplane.backend.service.impl;

import com.controlplane.backend.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailServiceImpl(JavaMailSender mailSender,
                             @Value("${app.mail.from}") String fromAddress,
                             @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String recipientName, String verificationToken) {
        String link = frontendUrl + "/verify-email?token=" + verificationToken;
        String body = """
                Hi %s,

                Thanks for creating an account on Backend Control Plane. Please confirm
                your email address by visiting the link below. This link expires in
                24 hours.

                %s

                If you did not create this account, you can safely ignore this email.
                """.formatted(recipientName, link);
        send(toEmail, "Verify your email address", body);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetToken) {
        String link = frontendUrl + "/reset-password?token=" + resetToken;
        String body = """
                Hi %s,

                We received a request to reset your password. This link is valid for
                30 minutes:

                %s

                If you did not request a password reset, no action is required and
                your password will remain unchanged.
                """.formatted(recipientName, link);
        send(toEmail, "Reset your password", body);
    }

    @Override
    @Async
    public void sendAccountLockedNotice(String toEmail, String recipientName) {
        String body = """
                Hi %s,

                Your account was temporarily locked after several failed login
                attempts. It will unlock automatically once the lockout period
                expires. If this was not you, we recommend resetting your password
                as soon as the account unlocks.
                """.formatted(recipientName);
        send(toEmail, "Your account has been temporarily locked", body);
    }

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String recipientName) {
        String body = """
                Hi %s,

                Welcome aboard! Your email address has been verified and your
                Backend Control Plane account is now fully active.

                You can sign in any time at:
                %s

                If you have any questions, just reply to this email.
                """.formatted(recipientName, frontendUrl);
        send(toEmail, "Welcome to Backend Control Plane", body);
    }

    @Override
    @Async
    public void sendNotificationEmail(String toEmail, String recipientName, String subject, String message) {
        String body = """
                Hi %s,

                %s

                View it in the app:
                %s
                """.formatted(recipientName, message, frontendUrl);
        send(toEmail, subject, body);
    }

    private void send(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException exception) {
            log.error("Failed to send email [{}] to {}", subject, toEmail, exception);
        }
    }
}
