package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.OtpVerification;
import com.employee.management.backend.repository.OtpVerificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final String mailFrom;
    private final long expirationMinutes;
    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(OtpVerificationRepository otpVerificationRepository,
                           EmailService emailService,
                           @Value("${app.mail.from}") String mailFrom,
                           @Value("${app.otp.expiration-minutes}") long expirationMinutes) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.emailService = emailService;
        this.mailFrom = mailFrom;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public void generateAndSendOtp(Employee employee) {
        otpVerificationRepository.invalidateActiveOtpsForEmployee(employee.getEmpId());

        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpVerification otpVerification = new OtpVerification(
                code, employee, LocalDateTime.now().plusMinutes(expirationMinutes));
        otpVerificationRepository.save(otpVerification);

        sendOtpEmail(employee, code);
    }

    @Override
    public void verifyOtp(Long empId, String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code is required");
        }

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmployee_EmpIdAndUsedFalseOrderByIdDesc(empId)
                .orElseThrow(() -> new IllegalArgumentException("No active verification code. Please request a new one."));

        if (!otpVerification.isValid()) {
            throw new IllegalArgumentException("Verification code has expired. Please request a new one.");
        }

        if (!otpVerification.getOtpCode().equals(otp.trim())) {
            otpVerification.setAttempts(otpVerification.getAttempts() + 1);
            if (otpVerification.getAttempts() >= MAX_ATTEMPTS) {
                otpVerification.setUsed(true);
                otpVerificationRepository.save(otpVerification);
                throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code.");
            }
            otpVerificationRepository.save(otpVerification);
            throw new IllegalArgumentException("Invalid verification code");
        }

        otpVerification.setUsed(true);
        otpVerificationRepository.save(otpVerification);
    }

    private void sendOtpEmail(Employee employee, String code) {
        String name = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
        String greeting = name.isEmpty() ? "Hello," : "Hello " + name + ",";

        String body = "<p>" + greeting + "</p>"
                + "<p>Your verification code is:</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:4px;\">" + code + "</p>"
                + "<p>This code expires in " + expirationMinutes + " minutes. If you didn't request this, "
                + "you can safely ignore this email.</p>";

        emailService.sendHtmlEmail(employee.getEmail(), mailFrom, null, "Your HRMS verification code", body);
    }
}
