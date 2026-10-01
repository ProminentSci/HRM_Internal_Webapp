package com.employee.management.backend.controller;

import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.dto.PayslipSettingsDTO;
import com.employee.management.backend.security.SecurityUtils;
import com.employee.management.backend.service.PayslipSettingsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/payslip-settings")
public class PayslipSettingsController {

    private final PayslipSettingsService payslipSettingsService;

    public PayslipSettingsController(PayslipSettingsService payslipSettingsService) {
        this.payslipSettingsService = payslipSettingsService;
    }

    // Any authenticated user (admin or employee) - the employee-facing Payslip page needs the
    // company branding/percentages to render its own payslip, not just the admin settings screen.
    @GetMapping
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(payslipSettingsService.getSettings(SecurityUtils.currentClientId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody PayslipSettingsDTO request) {
        try {
            return ResponseEntity.ok(payslipSettingsService.updateSettings(SecurityUtils.currentClientId(), request));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            payslipSettingsService.uploadLogo(SecurityUtils.currentClientId(), file);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // Any authenticated user - the employee Payslip page renders this as an <img src>.
    @GetMapping("/logo")
    public ResponseEntity<?> downloadLogo() {
        try {
            DocumentFile logo = payslipSettingsService.getLogo(SecurityUtils.currentClientId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + logo.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(logo.getContentType()))
                    .contentLength(logo.getSize())
                    .body(logo.getData());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
