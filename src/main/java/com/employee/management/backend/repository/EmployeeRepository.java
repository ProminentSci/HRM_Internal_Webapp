package com.employee.management.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.Employee;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);

    boolean existsByRoleIgnoreCase(String role);

    // Email is unique platform-wide (not per-client), so login-by-email needs no client filter -
    // the client's active/disabled status is checked separately once the employee is found.
    long countByClientId(Long clientId);

    Page<Employee> findByClientId(Long clientId, Pageable pageable);

    // Looks up an employee by the admin-assigned employee code within one tenant, to enforce
    // per-client uniqueness while allowing the same code to be reused across different clients.
    Optional<Employee> findByClientIdAndEmployeeId(Long clientId, String employeeId);

    // Resolves who should be emailed when an employee raises a ticket - every ADMIN-role
    // employee on that employee's own client, never a global address.
    List<Employee> findByClientIdAndRoleIgnoreCase(Long clientId, String role);

    // Used once, at startup, to migrate pre-multi-tenancy rows onto the default client.
    List<Employee> findByClientIsNull();

    // All list/search/filter queries below are scoped to :clientId so one company's admin can
    // never see another company's employees. clientId is always taken from the caller's own JWT
    // claim server-side, never a client-supplied parameter.
    @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE " +
            "e.client.id = :clientId AND " +
            "(:search IS NULL OR LOWER(e.firstName) LIKE :search OR LOWER(e.lastName) LIKE :search OR LOWER(COALESCE(jd.designation, '')) LIKE :search) AND " +
            "(:department IS NULL OR :department = '' OR TRIM(LOWER(COALESCE(jd.department, ''))) = TRIM(LOWER(:department))) AND " +
            "(:status IS NULL OR :status = '' OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) = TRIM(LOWER(:status))) AND " +
            "((:status IS NOT NULL AND TRIM(LOWER(:status)) = 'inactive') OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive') AND " +
            "(:employeeType IS NULL OR :employeeType = '' OR TRIM(LOWER(COALESCE(jd.employeeType, ''))) = TRIM(LOWER(:employeeType)))")
    Page<Employee> searchEmployees(@Param("clientId") Long clientId,
                                   @Param("search") String search,
                                   @Param("department") String department,
                                   @Param("status") String status,
                                   @Param("employeeType") String employeeType,
                                   Pageable pageable);

    @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE " +
            "e.client.id = :clientId AND " +
            "(:department IS NULL OR :department = '' OR TRIM(LOWER(COALESCE(jd.department, ''))) = TRIM(LOWER(:department))) AND " +
            "(:status IS NULL OR :status = '' OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) = TRIM(LOWER(:status))) AND " +
            "((:status IS NOT NULL AND TRIM(LOWER(:status)) = 'inactive') OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive')")
    Page<Employee> filterEmployees(@Param("clientId") Long clientId,
                                    @Param("department") String department,
                                    @Param("status") String status,
                                    Pageable pageable);

    // dateOfJoining is stored as a plain "yyyy-MM-dd" string, which sorts/compares
    // correctly as a string for ISO date ranges - no date parsing needed here.
    @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE " +
            "e.client.id = :clientId AND " +
            "(:department IS NULL OR :department = '' OR TRIM(LOWER(COALESCE(jd.department, ''))) = TRIM(LOWER(:department))) AND " +
            "(:status IS NULL OR :status = '' OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) = TRIM(LOWER(:status))) AND " +
            "((:status IS NOT NULL AND TRIM(LOWER(:status)) = 'inactive') OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive') AND " +
            "(:fromDate IS NULL OR :fromDate = '' OR jd.dateOfJoining >= :fromDate) AND " +
            "(:toDate IS NULL OR :toDate = '' OR jd.dateOfJoining <= :toDate)")
    Page<Employee> filterEmployeesByJoinDate(@Param("clientId") Long clientId,
                                              @Param("department") String department,
                                              @Param("status") String status,
                                              @Param("fromDate") String fromDate,
                                              @Param("toDate") String toDate,
                                              Pageable pageable);

    @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE e.client.id = :clientId AND TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive'")
    Page<Employee> findAllActive(@Param("clientId") Long clientId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Employee e LEFT JOIN e.jobDetails jd WHERE e.client.id = :clientId AND TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive'")
    long countActive(@Param("clientId") Long clientId);
}
