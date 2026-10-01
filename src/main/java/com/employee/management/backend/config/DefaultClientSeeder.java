package com.employee.management.backend.config;

import com.employee.management.backend.Entity.Client;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// One-time migration for the switch to multi-tenancy: any Employee row created before the
// Client concept existed has client_id = NULL, which would make it invisible to every
// client-scoped query (including its own admin's employee list). This backfills those rows onto
// the shared default client. Safe on every restart - and safe regardless of seeder run order,
// since AdminAccountSeeder/DemoDataSeeder now assign a client to every employee they create
// themselves; this only ever has legacy rows left to migrate.
@Component
public class DefaultClientSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final DefaultClientProvider defaultClientProvider;

    public DefaultClientSeeder(EmployeeRepository employeeRepository, DefaultClientProvider defaultClientProvider) {
        this.employeeRepository = employeeRepository;
        this.defaultClientProvider = defaultClientProvider;
    }

    @Override
    public void run(String... args) {
        List<Employee> orphaned = employeeRepository.findByClientIsNull();
        if (orphaned.isEmpty()) {
            return;
        }

        Client defaultClient = defaultClientProvider.getOrCreate();
        for (Employee employee : orphaned) {
            employee.setClient(defaultClient);
            employeeRepository.save(employee);
        }

        System.out.println("=================================================================");
        System.out.println("Migrated " + orphaned.size() + " pre-existing employee(s) onto client \""
                + defaultClient.getCompanyName() + "\" (id " + defaultClient.getId() + ").");
        System.out.println("=================================================================");
    }
}
