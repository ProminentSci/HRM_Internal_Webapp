package com.employee.management.backend.service;

import com.employee.management.backend.Entity.AddressDetails;
import com.employee.management.backend.Entity.DocumentDetails;
import com.employee.management.backend.Entity.EducationDetails;
import com.employee.management.backend.Entity.EmergencyContact;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.JobDetails;
import com.employee.management.backend.Entity.LeaveBalance;
import com.employee.management.backend.Entity.ProjectHistory;
import com.employee.management.backend.Entity.SalaryDetails;
import com.employee.management.backend.exception.DuplicateResourceException;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.DocumentDetailsRepository;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import com.employee.management.backend.dto.DocumentFile;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final DocumentDetailsRepository documentDetailsRepository;
    private final LeavePolicySettingsService leavePolicySettingsService;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, LeaveBalanceRepository leaveBalanceRepository,
                                DocumentDetailsRepository documentDetailsRepository,
                                LeavePolicySettingsService leavePolicySettingsService) {
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.documentDetailsRepository = documentDetailsRepository;
        this.leavePolicySettingsService = leavePolicySettingsService;
    }

    @Override
    public Page<Employee> findAllEmployees(Long clientId, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }
        int pageSize = Math.max(pageable.getPageSize(), 1);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Pageable safePageable = PageRequest.of(pageNumber, pageSize, pageable.getSort());
        return employeeRepository.findByClientId(clientId, safePageable);
    }

    @Override
    public Page<Employee> searchEmployees(Long clientId, String search, Pageable pageable) {
        return searchEmployees(clientId, search, null, null, null, pageable);
    }

    @Override
    public Page<Employee> searchEmployees(Long clientId, String search, String department, String status, String employeeType, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }

        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedDepartment = department == null ? "" : department.trim();
        String normalizedStatus = status == null ? "" : status.trim();
        String normalizedEmployeeType = employeeType == null ? "" : employeeType.trim();

        if (normalizedSearch.isEmpty() && normalizedDepartment.isEmpty() && normalizedStatus.isEmpty() && normalizedEmployeeType.isEmpty()) {
            return findAllEmployees(clientId, PageRequest.of(Math.max(pageable.getPageNumber(), 0), Math.max(pageable.getPageSize(), 1), pageable.getSort()));
        }

        if (!normalizedSearch.isEmpty() && isNumeric(normalizedSearch)) {
            Long employeeId = Long.parseLong(normalizedSearch);
            final int safePageNumber = Math.max(pageable.getPageNumber(), 0);
            final int safePageSize = Math.max(pageable.getPageSize(), 1);
            final var sort = pageable.getSort();
            final PageRequest pageRequest = PageRequest.of(safePageNumber, safePageSize, sort);
            final PageRequest singleResultRequest = PageRequest.of(0, 1, sort);
            // Matching by ID must still respect the tenant boundary - otherwise one client's
            // admin could enumerate another client's employee IDs through the search box.
            return employeeRepository.findById(employeeId)
                    .filter(employee -> employee.getClient() != null && clientId != null
                            && clientId.equals(employee.getClient().getId()))
                    .map(employee -> new PageImpl<Employee>(List.of(employee), singleResultRequest, 1))
                    .orElseGet(() -> new PageImpl<Employee>(List.of(), pageRequest, 0));
        }

        String searchPattern = normalizedSearch.isEmpty() ? null : "%" + normalizedSearch.toLowerCase() + "%";
        return employeeRepository.searchEmployees(clientId, searchPattern, normalizedDepartment, normalizedStatus, normalizedEmployeeType,
                PageRequest.of(Math.max(pageable.getPageNumber(), 0), Math.max(pageable.getPageSize(), 1), pageable.getSort()));
    }

    @Override
    public Page<Employee> filterEmployees(Long clientId, String department, String status, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }

        String normalizedDepartment = department == null ? "" : department.trim();
        String normalizedStatus = status == null ? "" : status.trim();
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int pageSize = Math.max(pageable.getPageSize(), 1);
        Pageable safePageable = PageRequest.of(pageNumber, pageSize, pageable.getSort());

        if (normalizedDepartment.isEmpty() && normalizedStatus.isEmpty()) {
            return findAllEmployees(clientId, safePageable);
        }

        return employeeRepository.filterEmployees(clientId, normalizedDepartment, normalizedStatus, safePageable);
    }

    @Override
    public Page<Employee> filterEmployeesByJoinDate(Long clientId, String department, String status, String fromDate, String toDate, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }

        String normalizedDepartment = department == null ? "" : department.trim();
        String normalizedStatus = status == null ? "" : status.trim();
        String normalizedFromDate = fromDate == null ? "" : fromDate.trim();
        String normalizedToDate = toDate == null ? "" : toDate.trim();
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int pageSize = Math.max(pageable.getPageSize(), 1);
        Pageable safePageable = PageRequest.of(pageNumber, pageSize, pageable.getSort());

        return employeeRepository.filterEmployeesByJoinDate(
                clientId, normalizedDepartment, normalizedStatus, normalizedFromDate, normalizedToDate, safePageable);
    }

    private boolean isNumeric(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    @Override
    public Employee findById(Long empId) {
        Employee employee = employeeRepository.findById(empId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));

        // initialize lazy associations while within the transactional boundary
        if (employee.getAddressDetails() != null) employee.getAddressDetails().getEmpId();
        if (employee.getJobDetails() != null) employee.getJobDetails().getEmpId();
        if (employee.getSalaryDetails() != null) employee.getSalaryDetails().getEmpId();
        if (employee.getEducationDetails() != null) employee.getEducationDetails().getEmpId();
        if (employee.getEmergencyContact() != null) employee.getEmergencyContact().getEmpId();
        if (employee.getDocumentDetails() != null) employee.getDocumentDetails().getEmpId();
        // initialize collections
        employee.getLeaveBalances().size();
        employee.getLeaveRequests().size();
        employee.getLeaveHistory().size();
        employee.getProjectHistory().size();

        return employee;
    }

    @Override
    public Employee createEmployee(Employee employee) {
        Long clientId = employee.getClient() != null ? employee.getClient().getId() : null;
        ensureUniqueEmployeeId(clientId, employee.getEmployeeId(), null);
        linkChildEntities(employee);
        syncProjectHistoryWithWorkStatus(employee, null, null);
        Employee saved = employeeRepository.save(employee);
        if (saved.getDocumentDetails() != null) {
            documentDetailsRepository.save(saved.getDocumentDetails());
        }
        seedDefaultLeaveBalances(saved);
        return saved;
    }

    // Employee ID uniqueness is scoped to (clientId, employeeId) - the same code is allowed to
    // repeat across different clients, since it identifies an employee within one company's own
    // numbering scheme, not platform-wide. A blank/absent employeeId is never checked - not every
    // employee needs one, and multiple NULLs coexist fine under the DB's unique index too.
    private void ensureUniqueEmployeeId(Long clientId, String employeeId, Long currentEmpId) {
        if (clientId == null || employeeId == null || employeeId.trim().isEmpty()) {
            return;
        }
        employeeRepository.findByClientIdAndEmployeeId(clientId, employeeId.trim())
                .filter(existing -> currentEmpId == null || !existing.getEmpId().equals(currentEmpId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Employee ID \"" + employeeId.trim() + "\" is already used by another employee in your organization");
                });
    }

    private void seedDefaultLeaveBalances(Employee employee) {
        Long clientId = employee.getClient() != null ? employee.getClient().getId() : null;
        String[] leaveTypes = LeavePolicySettingsService.LEAVE_TYPES;
        int[] leaveAllocations = leavePolicySettingsService.getAllocations(clientId);

        for (int i = 0; i < leaveTypes.length; i++) {
            LeaveBalance leaveBalance = new LeaveBalance();
            leaveBalance.setEmployee(employee);
            leaveBalance.setLeaveType(leaveTypes[i]);
            leaveBalance.setBalance(leaveAllocations[i]);
            leaveBalanceRepository.save(leaveBalance);
        }
    }

    @Override
    public Employee updateEmployee(Long empId, Employee employee) {
        Employee existing = findById(empId);
        Long clientId = existing.getClient() != null ? existing.getClient().getId() : null;
        ensureUniqueEmployeeId(clientId, employee.getEmployeeId(), empId);
        applyUpdates(existing, employee);
        linkChildEntities(existing);
        Employee saved = employeeRepository.save(existing);
        if (saved.getDocumentDetails() != null) {
            documentDetailsRepository.save(saved.getDocumentDetails());
        }
        return saved;
    }

    @Override
    public void updatePasswordHash(Long empId, String passwordHash) {
        Employee existing = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));
        existing.setPassword(passwordHash);
        employeeRepository.save(existing);
    }

    @Override
    public void deleteEmployee(Long empId) {
        Employee existing = findById(empId);
        employeeRepository.delete(existing);
    }

    @Override
    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "email", email));
    }

    @Override
    public void uploadEmployeeDocument(Long empId, String docType, MultipartFile file) {
        Employee employee = findById(empId);
        DocumentDetails details = employee.getDocumentDetails();
        if (details == null) {
            details = new DocumentDetails();
            details.setEmployee(employee);
            employee.setDocumentDetails(details);
        }

        try {
            byte[] data = file.getBytes();
            switch (docType.toLowerCase()) {
                case "resume":
                    details.setResumeUploadData(data);
                    details.setResumeUploadName(file.getOriginalFilename());
                    details.setResumeUploadContentType(file.getContentType());
                    break;
                case "idproof":
                    details.setIdProofUploadData(data);
                    details.setIdProofUploadName(file.getOriginalFilename());
                    details.setIdProofUploadContentType(file.getContentType());
                    break;
                case "addressproof":
                    details.setAddressProofUploadData(data);
                    details.setAddressProofUploadName(file.getOriginalFilename());
                    details.setAddressProofUploadContentType(file.getContentType());
                    break;
                case "education":
                    details.setEducationalCertificatesData(data);
                    details.setEducationalCertificatesName(file.getOriginalFilename());
                    details.setEducationalCertificatesContentType(file.getContentType());
                    break;
                case "experience":
                    details.setExperienceCertificatesData(data);
                    details.setExperienceCertificatesName(file.getOriginalFilename());
                    details.setExperienceCertificatesContentType(file.getContentType());
                    break;
                case "passport":
                    details.setPassportPhotoData(data);
                    details.setPassportPhotoName(file.getOriginalFilename());
                    details.setPassportPhotoContentType(file.getContentType());
                    break;
                case "tenthcertificate":
                    details.setTenthCertificateData(data);
                    details.setTenthCertificateName(file.getOriginalFilename());
                    details.setTenthCertificateContentType(file.getContentType());
                    break;
                case "intermediatemarksheet":
                    details.setIntermediateMarksheetData(data);
                    details.setIntermediateMarksheetName(file.getOriginalFilename());
                    details.setIntermediateMarksheetContentType(file.getContentType());
                    break;
                case "provisionalcertificate":
                    details.setProvisionalCertificateData(data);
                    details.setProvisionalCertificateName(file.getOriginalFilename());
                    details.setProvisionalCertificateContentType(file.getContentType());
                    break;
                case "originaldegree":
                    details.setOriginalDegreeData(data);
                    details.setOriginalDegreeName(file.getOriginalFilename());
                    details.setOriginalDegreeContentType(file.getContentType());
                    break;
                case "aadhaarupload":
                    details.setAadhaarUploadData(data);
                    details.setAadhaarUploadName(file.getOriginalFilename());
                    details.setAadhaarUploadContentType(file.getContentType());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported document type: " + docType);
            }
            // Saved directly through its own repository (rather than only cascading via
            // employeeRepository.save) so a brand-new @MapsId DocumentDetails row is
            // guaranteed to flush - relying solely on cascade-through-merge here was
            // silently not persisting the row for employees that had no prior document.
            documentDetailsRepository.save(details);
        } catch (Exception ex) {
            throw new RuntimeException("Could not store document file", ex);
        }
    }

    @Override
    public DocumentFile getEmployeeDocument(Long empId, String docType) {
        Employee employee = findById(empId);
        DocumentDetails details = employee.getDocumentDetails();
        if (details == null) {
            throw new ResourceNotFoundException("DocumentDetails", "empId", empId);
        }

        switch (docType.toLowerCase()) {
            case "resume":
                return toDocumentFile(details.getResumeUploadData(), details.getResumeUploadName(), details.getResumeUploadContentType());
            case "idproof":
                return toDocumentFile(details.getIdProofUploadData(), details.getIdProofUploadName(), details.getIdProofUploadContentType());
            case "addressproof":
                return toDocumentFile(details.getAddressProofUploadData(), details.getAddressProofUploadName(), details.getAddressProofUploadContentType());
            case "education":
                return toDocumentFile(details.getEducationalCertificatesData(), details.getEducationalCertificatesName(), details.getEducationalCertificatesContentType());
            case "experience":
                return toDocumentFile(details.getExperienceCertificatesData(), details.getExperienceCertificatesName(), details.getExperienceCertificatesContentType());
            case "passport":
                return toDocumentFile(details.getPassportPhotoData(), details.getPassportPhotoName(), details.getPassportPhotoContentType());
            case "tenthcertificate":
                return toDocumentFile(details.getTenthCertificateData(), details.getTenthCertificateName(), details.getTenthCertificateContentType());
            case "intermediatemarksheet":
                return toDocumentFile(details.getIntermediateMarksheetData(), details.getIntermediateMarksheetName(), details.getIntermediateMarksheetContentType());
            case "provisionalcertificate":
                return toDocumentFile(details.getProvisionalCertificateData(), details.getProvisionalCertificateName(), details.getProvisionalCertificateContentType());
            case "originaldegree":
                return toDocumentFile(details.getOriginalDegreeData(), details.getOriginalDegreeName(), details.getOriginalDegreeContentType());
            case "aadhaarupload":
                return toDocumentFile(details.getAadhaarUploadData(), details.getAadhaarUploadName(), details.getAadhaarUploadContentType());
            default:
                throw new IllegalArgumentException("Unsupported document type: " + docType);
        }
    }

    private DocumentFile toDocumentFile(byte[] data, String fileName, String contentType) {
        if (data == null || fileName == null) {
            throw new ResourceNotFoundException("Document file", "name", fileName);
        }
        return new DocumentFile(fileName, contentType == null ? "application/octet-stream" : contentType, data.length, data);
    }

    private void applyUpdates(Employee existing, Employee incoming) {
        existing.setEmployeeId(incoming.getEmployeeId());
        existing.setFirstName(incoming.getFirstName());
        existing.setLastName(incoming.getLastName());
        existing.setEmail(incoming.getEmail());
        existing.setPhone(incoming.getPhone());
        existing.setAlternatePhone(incoming.getAlternatePhone());
        existing.setGender(incoming.getGender());
        existing.setDateOfBirth(incoming.getDateOfBirth());
        existing.setBloodGroup(incoming.getBloodGroup());
        existing.setMaritalStatus(incoming.getMaritalStatus());
        existing.setNationality(incoming.getNationality());
       // existing.setPassword(incoming.getPassword());
        existing.setRole(incoming.getRole());
        existing.setProfilePhoto(incoming.getProfilePhoto());

        // Capture the work-status/current-project state as it was before this save, so we can
        // tell afterwards whether the employee just moved onto or off of a project.
        JobDetails previousJobDetails = existing.getJobDetails();
        String previousWorkStatus = previousJobDetails != null ? previousJobDetails.getWorkStatus() : null;
        String previousProjectName = previousJobDetails != null ? previousJobDetails.getCurrentProjectName() : null;

        // Update child entities instead of replacing them to avoid Hibernate session conflicts
        updateAddressDetails(existing, incoming.getAddressDetails());
        updateJobDetails(existing, incoming.getJobDetails());
        updateSalaryDetails(existing, incoming.getSalaryDetails());
        updateEducationDetails(existing, incoming.getEducationDetails());
        updateEmergencyContact(existing, incoming.getEmergencyContact());
        updateDocumentDetails(existing, incoming.getDocumentDetails());
        updateProjectHistory(existing, incoming.getProjectHistory());
        syncProjectHistoryWithWorkStatus(existing, previousWorkStatus, previousProjectName);
    }

    // Keeps the project-history table in sync with the "current project" fields on JobDetails,
    // so an admin doesn't have to manually duplicate the same info into both places:
    //  - Assigning someone to a project (workStatus -> "Project") opens a history record for it,
    //    if one isn't already open.
    //  - Moving someone off a project (workStatus -> "Bench") closes out that project's open
    //    history record by stamping today's date as its end date.
    private void syncProjectHistoryWithWorkStatus(Employee employee, String previousWorkStatus, String previousProjectName) {
        JobDetails jobDetails = employee.getJobDetails();
        if (jobDetails == null) {
            return;
        }

        boolean wasOnProject = "Project".equalsIgnoreCase(previousWorkStatus);
        boolean isOnProject = "Project".equalsIgnoreCase(jobDetails.getWorkStatus());
        List<ProjectHistory> history = employee.getProjectHistory();
        String today = java.time.LocalDate.now().toString();

        if (wasOnProject && !isOnProject) {
            history.stream()
                    .filter(entry -> entry.getEndDate() == null || entry.getEndDate().isBlank())
                    .filter(entry -> previousProjectName == null || previousProjectName.equalsIgnoreCase(entry.getProjectName()))
                    .findFirst()
                    .ifPresent(entry -> entry.setEndDate(today));
        }

        String currentProjectName = jobDetails.getCurrentProjectName();
        if (isOnProject && currentProjectName != null && !currentProjectName.isBlank()) {
            boolean hasOpenRecordForCurrentProject = history.stream()
                    .anyMatch(entry -> currentProjectName.equalsIgnoreCase(entry.getProjectName())
                            && (entry.getEndDate() == null || entry.getEndDate().isBlank()));

            if (!hasOpenRecordForCurrentProject) {
                ProjectHistory entry = new ProjectHistory();
                entry.setEmployee(employee);
                entry.setProjectName(currentProjectName);
                entry.setProjectManager(jobDetails.getCurrentProjectManager());
                String startDate = jobDetails.getCurrentProjectStartDate();
                entry.setStartDate(startDate != null && !startDate.isBlank() ? startDate : today);
                entry.setEndDate(null);
                history.add(entry);
            }
        }
    }

    private void updateAddressDetails(Employee employee, AddressDetails incoming) {
        if (incoming == null) {
            employee.setAddressDetails(null);
            return;
        }
        
        AddressDetails existing = employee.getAddressDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setAddressDetails(incoming);
        } else {
            existing.setPermanentAddress(incoming.getPermanentAddress());
            existing.setCurrentAddress(incoming.getCurrentAddress());
            existing.setCountry(incoming.getCountry());
            existing.setState(incoming.getState());
            existing.setCity(incoming.getCity());
            existing.setPincode(incoming.getPincode());
        }
    }

    private void updateJobDetails(Employee employee, JobDetails incoming) {
        if (incoming == null) {
            employee.setJobDetails(null);
            return;
        }
        
        JobDetails existing = employee.getJobDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setJobDetails(incoming);
        } else {
            existing.setDesignation(incoming.getDesignation());
            existing.setDepartment(incoming.getDepartment());
            existing.setEmployeeType(incoming.getEmployeeType());
            existing.setReportingManager(incoming.getReportingManager());
            existing.setDateOfJoining(incoming.getDateOfJoining());
            existing.setWorkLocation(incoming.getWorkLocation());
            existing.setEmployeeStatus(incoming.getEmployeeStatus());
            existing.setShiftTiming(incoming.getShiftTiming());
            existing.setExperience(incoming.getExperience());
            existing.setEmployeeCategory(incoming.getEmployeeCategory());
            existing.setWorkStatus(incoming.getWorkStatus());
            existing.setCurrentProjectName(incoming.getCurrentProjectName());
            existing.setCurrentProjectManager(incoming.getCurrentProjectManager());
            existing.setCurrentProjectStartDate(incoming.getCurrentProjectStartDate());
            existing.setPositionLevel(incoming.getPositionLevel());
        }
    }

    private void updateSalaryDetails(Employee employee, SalaryDetails incoming) {
        if (incoming == null) {
            employee.setSalaryDetails(null);
            return;
        }
        
        SalaryDetails existing = employee.getSalaryDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setSalaryDetails(incoming);
        } else {
            existing.setBasicSalary(incoming.getBasicSalary());
            existing.setBonus(incoming.getBonus());
            existing.setCtc(incoming.getCtc());
            existing.setPfApplicable(incoming.getPfApplicable());
            existing.setPfNumber(incoming.getPfNumber());
            existing.setEsiApplicable(incoming.getEsiApplicable());
            existing.setBankName(incoming.getBankName());
            existing.setAccountNumber(incoming.getAccountNumber());
            existing.setIfscCode(incoming.getIfscCode());
            existing.setBranchName(incoming.getBranchName());
            existing.setPanNumber(incoming.getPanNumber());
            existing.setAadhaarNumber(incoming.getAadhaarNumber());
            existing.setUanNumber(incoming.getUanNumber());
        }
    }

    private void updateEducationDetails(Employee employee, EducationDetails incoming) {
        if (incoming == null) {
            employee.setEducationDetails(null);
            return;
        }
        
        EducationDetails existing = employee.getEducationDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setEducationDetails(incoming);
        } else {
            existing.setHighestQualification(incoming.getHighestQualification());
            existing.setUniversityCollege(incoming.getUniversityCollege());
            existing.setYearOfPassing(incoming.getYearOfPassing());
            existing.setPercentageCGPA(incoming.getPercentageCGPA());
        }
    }

    private void updateEmergencyContact(Employee employee, EmergencyContact incoming) {
        if (incoming == null) {
            employee.setEmergencyContact(null);
            return;
        }
        
        EmergencyContact existing = employee.getEmergencyContact();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setEmergencyContact(incoming);
        } else {
            existing.setEmergencyContactName(incoming.getEmergencyContactName());
            existing.setRelationship(incoming.getRelationship());
            existing.setEmergencyContactNumber(incoming.getEmergencyContactNumber());
            existing.setEmergencyAlternateNumber(incoming.getEmergencyAlternateNumber());
            existing.setEmergencyAddress(incoming.getEmergencyAddress());
        }
    }

    private void updateDocumentDetails(Employee employee, DocumentDetails incoming) {
        if (incoming == null) {
            employee.setDocumentDetails(null);
            return;
        }
        
        DocumentDetails existing = employee.getDocumentDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setDocumentDetails(incoming);
        } else {
            existing.setResumeUpload(incoming.getResumeUpload());
            existing.setIdProofUpload(incoming.getIdProofUpload());
            existing.setAddressProofUpload(incoming.getAddressProofUpload());
            existing.setEducationalCertificates(incoming.getEducationalCertificates());
            existing.setExperienceCertificates(incoming.getExperienceCertificates());
            existing.setPassportPhoto(incoming.getPassportPhoto());
            existing.setPassportNumber(incoming.getPassportNumber());
            existing.setDrivingLicense(incoming.getDrivingLicense());
            existing.setLicenseExpiryDate(incoming.getLicenseExpiryDate());
        }
    }

    private void updateProjectHistory(Employee employee, List<ProjectHistory> incomingList) {
        // The edit form always resubmits the complete current list, so replace the managed
        // collection's contents in place (never reassign the reference) - Hibernate relies on
        // that to correctly delete orphaned rows and insert new ones via cascade/orphanRemoval.
        List<ProjectHistory> existingList = employee.getProjectHistory();
        existingList.clear();

        if (incomingList == null) {
            return;
        }

        for (ProjectHistory incoming : incomingList) {
            ProjectHistory entry = new ProjectHistory();
            entry.setEmployee(employee);
            entry.setProjectName(incoming.getProjectName());
            entry.setProjectManager(incoming.getProjectManager());
            entry.setStartDate(incoming.getStartDate());
            entry.setEndDate(incoming.getEndDate());
            existingList.add(entry);
        }
    }

    private void linkChildEntities(Employee employee) {
        if (employee == null) {
            return;
        }

        if (employee.getAddressDetails() != null) {
            employee.getAddressDetails().setEmployee(employee);
        }
        if (employee.getJobDetails() != null) {
            employee.getJobDetails().setEmployee(employee);
        }
        if (employee.getSalaryDetails() != null) {
            employee.getSalaryDetails().setEmployee(employee);
        }
        if (employee.getEducationDetails() != null) {
            employee.getEducationDetails().setEmployee(employee);
        }
        if (employee.getEmergencyContact() != null) {
            employee.getEmergencyContact().setEmployee(employee);
        }
        if (employee.getDocumentDetails() != null) {
            employee.getDocumentDetails().setEmployee(employee);
        }
        if (employee.getProjectHistory() != null) {
            employee.getProjectHistory().forEach(entry -> entry.setEmployee(employee));
        }
    }
}
