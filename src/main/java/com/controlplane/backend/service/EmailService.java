package com.controlplane.backend.service;

/**
 * Sends the transactional emails triggered by the authentication flow.
 * Kept as a narrow interface so the SMTP-backed implementation can be
 * swapped for a provider like SES or SendGrid without touching
 * {@code AuthServiceImpl}.
 */
public interface EmailService {

    void sendVerificationEmail(String toEmail, String recipientName, String verificationToken);

    void sendPasswordResetEmail(String toEmail, String recipientName, String resetToken);

    void sendAccountLockedNotice(String toEmail, String recipientName);

    void sendWelcomeEmail(String toEmail, String recipientName);

    void sendNotificationEmail(String toEmail, String recipientName, String subject, String message);
}
