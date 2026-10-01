//package com.employee.management.backend.controller;
//
//import com.employee.management.backend.Entity.Employee;
//import com.employee.management.backend.Entity.JobDetails;
//import com.employee.management.backend.service.EmployeeService;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(ReportsController.class)
//class ReportsControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private EmployeeService employeeService;
//
//    @Test
//    void shouldReturnFlattenedEmployeeReportFields() throws Exception {
//        Employee employee = new Employee();
//        employee.setEmpId(10L);
//        employee.setFirstName("Asha");
//        employee.setLastName("Rao");
//        employee.setEmail("asha@example.com");
//
//        JobDetails jobDetails = new JobDetails();
//        jobDetails.setDepartment("HR");
//        jobDetails.setDesignation("Manager");
//        jobDetails.setEmployeeStatus("active");
//        employee.setJobDetails(jobDetails);
//
//        Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(0, 10), 1);
//        when(employeeService.searchEmployees(any(), any(), any(), any())).thenReturn(page);
//
//        mockMvc.perform(get("/api/reports/employees")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .param("department", "HR")
//                        .param("status", "active")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content[0].empId").value(10))
//                .andExpect(jsonPath("$.content[0].fullName").value("Asha Rao"))
//                .andExpect(jsonPath("$.content[0].department").value("HR"))
//                .andExpect(jsonPath("$.content[0].status").value("active"));
//
//        ArgumentCaptor<String> departmentCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
//        verify(employeeService).searchEmployees(any(), departmentCaptor.capture(), statusCaptor.capture(), any(Pageable.class));
//        assertThat(departmentCaptor.getValue()).isEqualTo("HR");
//        assertThat(statusCaptor.getValue()).isEqualTo("active");
//    }
//}
