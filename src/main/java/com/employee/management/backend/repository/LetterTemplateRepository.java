package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.LetterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LetterTemplateRepository extends JpaRepository<LetterTemplate, Long> {
    List<LetterTemplate> findByClientIdAndLetterTypeOrderByNameAsc(Long clientId, String letterType);

    Optional<LetterTemplate> findByIdAndClientId(Long id, Long clientId);
}
