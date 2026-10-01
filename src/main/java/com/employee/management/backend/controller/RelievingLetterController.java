package com.employee.management.backend.controller;

import com.employee.management.backend.dto.EmployeeLetterResponseDTO;
import com.employee.management.backend.service.EmployeeLetterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class RelievingLetterController {
    private final EmployeeLetterService employeeLetterService;

    public RelievingLetterController(EmployeeLetterService employeeLetterService) {
        this.employeeLetterService = employeeLetterService;
    }

    @GetMapping("/employee/{empId}/relieving")
    public ResponseEntity<EmployeeLetterResponseDTO> getRelievingLetterDetails(
            @PathVariable Long empId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate relievingDate) {
        return ResponseEntity.ok(employeeLetterService.getLetterDetails(empId, relievingDate));
    }
}
