//package com.employee.management.backend.controller;
//
//import com.employee.management.backend.Entity.Employee;
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
//@WebMvcTest(EmployeeController.class)
//class EmployeeControllerPaginationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private EmployeeService employeeService;
//
//    @Test
//    void shouldFilterEmployeesByDepartmentAndStatus() throws Exception {
//        Employee employee = new Employee();
//        employee.setEmpId(1L);
//        employee.setFirstName("Alice");
//        employee.setLastName("Smith");
//        employee.setEmail("alice@example.com");
//        employee.setRole("employee");
//
//        Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(1, 5), 6);
//        when(employeeService.searchEmployees(any(), any(), any(), any())).thenReturn(page);
//
//        mockMvc.perform(get("/api/employees")
//                        .param("page", "1")
//                        .param("size", "5")
//                        .param("department", "HR")
//                        .param("status", "active")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content[0].empId").value(1))
//                .andExpect(jsonPath("$.content[0].firstName").value("Alice"))
//                .andExpect(jsonPath("$.totalElements").value(6));
//
//        ArgumentCaptor<String> departmentCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
//        verify(employeeService).searchEmployees(any(), departmentCaptor.capture(), statusCaptor.capture(), pageableCaptor.capture());
//        assertThat(departmentCaptor.getValue()).isEqualTo("HR");
//        assertThat(statusCaptor.getValue()).isEqualTo("active");
//        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
//        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
//    }
//}
