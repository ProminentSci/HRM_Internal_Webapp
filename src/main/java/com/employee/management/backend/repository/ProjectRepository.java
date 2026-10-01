package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderByNameAsc();

    List<Project> findAllByClientIdOrderByNameAsc(Long clientId);

    Optional<Project> findByIdAndClientId(Long id, Long clientId);

    List<Project> findByProjectManagerEmpId(Long empId);

    Optional<Project> findByName(String name);
}
