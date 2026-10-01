package com.employee.management.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.Ticket;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByEmployeeEmpIdOrderByCreatedAtDesc(Long empId);

    // Scoped to the admin's own client so one company's admin can never see or touch
    // another company's tickets - same isolation principle as EmployeeRepository's
    // clientId-scoped queries.
    @Query("SELECT t FROM Ticket t JOIN t.employee e WHERE e.client.id = :clientId " +
            "AND (:status IS NULL OR :status = '' OR TRIM(LOWER(t.status)) = TRIM(LOWER(:status))) " +
            "ORDER BY t.createdAt DESC")
    Page<Ticket> filterForClient(@Param("clientId") Long clientId, @Param("status") String status, Pageable pageable);
}
