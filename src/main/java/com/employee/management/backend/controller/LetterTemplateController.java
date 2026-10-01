package com.employee.management.backend.controller;

import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.dto.LetterTemplateDTO;
import com.employee.management.backend.security.SecurityUtils;
import com.employee.management.backend.service.LetterTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// Every letter-generating page under /admin/essentials is admin-only, so template management
// stays admin-only too - unlike Payslip Settings, employees never need to read these.
@RestController
@RequestMapping("/api/letter-templates")
@PreAuthorize("hasRole('ADMIN')")
public class LetterTemplateController {

    private final LetterTemplateService letterTemplateService;

    public LetterTemplateController(LetterTemplateService letterTemplateService) {
        this.letterTemplateService = letterTemplateService;
    }

    @GetMapping
    public ResponseEntity<?> listTemplates(@RequestParam String letterType) {
        try {
            return ResponseEntity.ok(letterTemplateService.listTemplates(SecurityUtils.currentClientId(), letterType));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(path = "/parse-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> parseDocument(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(letterTemplateService.parseDocument(file));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody LetterTemplateDTO request) {
        try {
            return ResponseEntity.ok(letterTemplateService.createTemplate(SecurityUtils.currentClientId(), request));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody LetterTemplateDTO request) {
        try {
            return ResponseEntity.ok(letterTemplateService.updateTemplate(id, SecurityUtils.currentClientId(), request));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            letterTemplateService.deleteTemplate(id, SecurityUtils.currentClientId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(path = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            letterTemplateService.uploadLogo(id, SecurityUtils.currentClientId(), file);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<?> downloadLogo(@PathVariable Long id) {
        try {
            DocumentFile logo = letterTemplateService.getLogo(id, SecurityUtils.currentClientId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + logo.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(logo.getContentType()))
                    .contentLength(logo.getSize())
                    .body(logo.getData());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(path = "/{id}/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSignature(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            letterTemplateService.uploadSignature(id, SecurityUtils.currentClientId(), file);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}/signature")
    public ResponseEntity<?> downloadSignature(@PathVariable Long id) {
        try {
            DocumentFile signature = letterTemplateService.getSignature(id, SecurityUtils.currentClientId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + signature.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(signature.getContentType()))
                    .contentLength(signature.getSize())
                    .body(signature.getData());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
