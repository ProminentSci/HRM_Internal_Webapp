package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import com.employee.management.backend.dto.DocumentFile;

public interface EmployeeService {
    Page<Employee> findAllEmployees(Long clientId, Pageable pageable);

    Page<Employee> searchEmployees(Long clientId, String search, Pageable pageable);

    Page<Employee> searchEmployees(Long clientId, String search, String department, String status, String employeeType, Pageable pageable);

    Page<Employee> filterEmployees(Long clientId, String department, String status, Pageable pageable);

    Page<Employee> filterEmployeesByJoinDate(Long clientId, String department, String status, String fromDate, String toDate, Pageable pageable);

    Employee findById(Long empId);

    Employee createEmployee(Employee employee);

    Employee updateEmployee(Long empId, Employee employee);

    void updatePasswordHash(Long empId, String passwordHash);

    void deleteEmployee(Long empId);

    Employee findByEmail(String email);

    void uploadEmployeeDocument(Long empId, String docType, MultipartFile file);

    DocumentFile getEmployeeDocument(Long empId, String docType);
}
