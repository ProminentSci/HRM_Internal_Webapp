package com.employee.management.backend.config;

import com.employee.management.backend.Entity.SuperAdmin;
import com.employee.management.backend.repository.SuperAdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates a single default Super Admin account on startup if none exists yet, mirroring
// AdminAccountSeeder's pattern for the tenant-level admin account. Safe to run on every
// restart: it's a no-op once any Super Admin exists.
@Component
public class SuperAdminSeeder implements CommandLineRunner {

    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;

    public SuperAdminSeeder(SuperAdminRepository superAdminRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.super-admin.seed-email:superadmin@hrms.local}") String seedEmail,
                             @Value("${app.super-admin.seed-password:SuperAdmin@123}") String seedPassword) {
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (superAdminRepository.count() > 0) {
            return;
        }

        SuperAdmin admin = new SuperAdmin();
        admin.setName("Platform Owner");
        admin.setEmail(seedEmail);
        admin.setPassword(passwordEncoder.encode(seedPassword));
        superAdminRepository.save(admin);

        System.out.println("=================================================================");
        System.out.println("No Super Admin account existed - created a default one:");
        System.out.println("  Email:    " + seedEmail);
        System.out.println("  Password: " + seedPassword);
        System.out.println("Log in at /super-admin/login and change the password.");
        System.out.println("Override app.super-admin.seed-email / app.super-admin.seed-password to change what gets created here.");
        System.out.println("=================================================================");
    }
}
