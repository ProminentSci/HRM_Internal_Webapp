package com.employee.management.backend.config;

import com.employee.management.backend.Entity.Client;
import com.employee.management.backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Shared by every seeder that creates an Employee before any specific Client exists yet
// (AdminAccountSeeder, DemoDataSeeder), so newly-seeded accounts are never left with client_id
// NULL regardless of which seeder happens to run first. DefaultClientSeeder reuses this too, to
// migrate any employee rows that predate the Client concept entirely.
@Component
public class DefaultClientProvider {

    private final ClientRepository clientRepository;
    private final String defaultClientName;

    public DefaultClientProvider(ClientRepository clientRepository,
                                  @Value("${app.default-client.name:Default Company}") String defaultClientName) {
        this.clientRepository = clientRepository;
        this.defaultClientName = defaultClientName;
    }

    public Client getOrCreate() {
        return clientRepository.findAllByOrderByCompanyNameAsc().stream()
                .filter(c -> defaultClientName.equalsIgnoreCase(c.getCompanyName()))
                .findFirst()
                .orElseGet(() -> {
                    Client client = new Client();
                    client.setCompanyName(defaultClientName);
                    client.setStatus("Active");
                    client.setCreatedAt(LocalDateTime.now());
                    return clientRepository.save(client);
                });
    }
}
