package com.employee.management.backend.service;

public interface PasswordResetService {
    // Never throws for an unknown email - the caller can't tell whether the address is
    // registered, to avoid leaking account existence through this endpoint.
    void requestReset(String email);

    void resetPassword(String token, String newPassword);
}
