package com.employee.management.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.LeaveRequest;

import java.util.Collection;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeEmpIdOrderByCreatedAtDesc(Long empId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    List<LeaveRequest> findAllByEmployeeClientIdOrderByCreatedAtDesc(Long clientId);
    List<LeaveRequest> findByEmployeeEmpIdInOrderByCreatedAtDesc(Collection<Long> empIds);

    @Query("SELECT lr FROM LeaveRequest lr JOIN lr.employee e WHERE " +
            "e.client.id = :clientId AND " +
            "(:status IS NULL OR :status = '' OR TRIM(LOWER(lr.status)) = TRIM(LOWER(:status))) AND " +
            "(:searchId IS NULL OR e.empId = :searchId) AND " +
            "(:searchName IS NULL OR :searchName = '' OR " +
            "  LOWER(CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, ''))) LIKE LOWER(CONCAT('%', :searchName, '%'))) AND " +
            "(:year IS NULL OR :month IS NULL OR " +
            "  (FUNCTION('YEAR', lr.fromDate) = :year AND FUNCTION('MONTH', lr.fromDate) = :month) OR " +
            "  (FUNCTION('YEAR', lr.toDate) = :year AND FUNCTION('MONTH', lr.toDate) = :month)) " +
            "ORDER BY lr.createdAt DESC")
    Page<LeaveRequest> filterLeaveRequests(@Param("clientId") Long clientId,
                                            @Param("status") String status,
                                            @Param("searchId") Long searchId,
                                            @Param("searchName") String searchName,
                                            @Param("year") Integer year,
                                            @Param("month") Integer month,
                                            Pageable pageable);
}
