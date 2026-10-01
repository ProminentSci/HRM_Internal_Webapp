package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;

public interface OtpService {
    // Invalidates any active OTP for this employee, generates a new 6-digit code, and emails it.
    void generateAndSendOtp(Employee employee);

    // Throws IllegalArgumentException with a user-facing message if the code is missing, wrong,
    // expired, or the max attempt count has been exceeded. Marks the OTP used on success.
    void verifyOtp(Long empId, String otp);
}
