package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findAllByOrderByCompanyNameAsc();

    boolean existsByCompanyNameIgnoreCase(String companyName);
}
