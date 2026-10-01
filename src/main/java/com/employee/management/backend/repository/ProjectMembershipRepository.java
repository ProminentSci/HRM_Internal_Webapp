package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, Long> {
    List<ProjectMembership> findByProjectId(Long projectId);

    List<ProjectMembership> findByEmployeeEmpId(Long empId);

    Optional<ProjectMembership> findByProjectIdAndEmployeeEmpId(Long projectId, Long empId);

    boolean existsByEmployeeEmpId(Long empId);

    boolean existsByEmployeeEmpIdAndEndDateIsNull(Long empId);

    // "On bench" = no OPEN membership anywhere, AND not managing any still-active project either
    // (a project's PM has no ProjectMembership row of their own - they're tracked only via
    // Project.projectManager). A membership/PM slot on a since-completed project no longer
    // counts, even though the row/assignment itself stays for historical/roster purposes.
    // Scoped to :clientId so the bench roster only ever shows one tenant's own employees.
    @Query("SELECT e FROM Employee e WHERE e.client.id = :clientId AND e NOT IN "
            + "(SELECT pm.employee FROM ProjectMembership pm WHERE pm.endDate IS NULL) "
            + "AND e NOT IN (SELECT p.projectManager FROM Project p "
            + "WHERE p.projectManager IS NOT NULL AND (p.status IS NULL OR p.status <> 'Completed'))")
    List<Employee> findEmployeesNotOnAnyProject(@Param("clientId") Long clientId);
}
