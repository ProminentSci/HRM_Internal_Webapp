package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.PasswordResetToken;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private final EmployeeRepository employeeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;
    private final String mailFrom;
    private final long expirationMinutes;

    public PasswordResetServiceImpl(EmployeeRepository employeeRepository,
                                     PasswordResetTokenRepository tokenRepository,
                                     PasswordEncoder passwordEncoder,
                                     EmailService emailService,
                                     @Value("${app.frontend-url}") String frontendUrl,
                                     @Value("${app.mail.from}") String mailFrom,
                                     @Value("${app.password-reset.expiration-minutes}") long expirationMinutes) {
        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public void requestReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        employeeRepository.findByEmail(email.trim()).ifPresent(employee -> {
            tokenRepository.invalidateActiveTokensForEmployee(employee.getEmpId());

            String token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken resetToken = new PasswordResetToken(
                    token, employee, LocalDateTime.now().plusMinutes(expirationMinutes));
            tokenRepository.save(resetToken);

            sendResetEmail(employee, token);
        });
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link"));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired reset link");
        }

        Employee employee = resetToken.getEmployee();
        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void sendResetEmail(Employee employee, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String name = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();

        emailService.sendHtmlEmail(employee.getEmail(), mailFrom, null,
                "Reset your HRMS password", buildEmailBody(name, resetLink));
    }

    private String buildEmailBody(String name, String resetLink) {
        String greeting = name.isEmpty() ? "Hello," : "Hello " + name + ",";
        return "<p>" + greeting + "</p>"
                + "<p>We received a request to reset your HRMS password. Click the link below to choose a new password:</p>"
                + "<p><a href=\"" + resetLink + "\">" + resetLink + "</a></p>"
                + "<p>This link expires in " + expirationMinutes + " minutes. If you didn't request this, "
                + "you can safely ignore this email.</p>";
    }
}
