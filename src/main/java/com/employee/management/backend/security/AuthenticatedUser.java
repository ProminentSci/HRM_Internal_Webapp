package com.employee.management.backend.security;

// clientId is null for a super_admin token (a Super Admin isn't scoped to any tenant); for
// admin/employee tokens it's the tenant company they belong to and drives every data-isolation
// check server-side.
public record AuthenticatedUser(Long empId, String email, String role, Long clientId) {
}
