package com.employee.management.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends mail on the "mailTaskExecutor" pool so SMTP latency/timeouts never
 * block the request thread that triggered the email (login, leave request,
 * password reset, etc). Failures are logged here since, being async, there's
 * no caller left waiting to receive them.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("mailTaskExecutor")
    public void sendHtmlEmail(String to, String from, String replyTo, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            if (replyTo != null && !replyTo.trim().isEmpty()) {
                helper.setReplyTo(replyTo);
            }
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            System.out.println("Failed to send email to " + to + " - "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
}
