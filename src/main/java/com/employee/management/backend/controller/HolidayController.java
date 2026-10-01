package com.employee.management.backend.controller;

import com.employee.management.backend.dto.CreateHolidayDTO;
import com.employee.management.backend.dto.HolidayDTO;
import com.employee.management.backend.dto.UpdateHolidayDTO;
import com.employee.management.backend.security.AuthenticatedUser;
import com.employee.management.backend.security.SecurityUtils;
import com.employee.management.backend.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public ResponseEntity<List<HolidayDTO>> listHolidays(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Integer upcomingDays) {
        return ResponseEntity.ok(holidayService.getHolidays(SecurityUtils.currentClientId(), year, start, end, upcomingDays));
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getYears() {
        return ResponseEntity.ok(holidayService.getHolidayYears(SecurityUtils.currentClientId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolidayDTO> getHoliday(@PathVariable Long id) {
        return ResponseEntity.ok(holidayService.getHoliday(id, SecurityUtils.currentClientId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HolidayDTO> createHoliday(
            Authentication authentication,
            @Valid @RequestBody CreateHolidayDTO request) {
        HolidayDTO holidayDTO = holidayService.createHoliday(request, currentUserLabel(authentication), SecurityUtils.currentClientId());
        return ResponseEntity.created(URI.create("/api/holidays/" + holidayDTO.getId()))
                .body(holidayDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<HolidayDTO> updateHoliday(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody UpdateHolidayDTO request) {
        return ResponseEntity.ok(holidayService.updateHoliday(id, request, currentUserLabel(authentication), SecurityUtils.currentClientId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id, SecurityUtils.currentClientId());
        return ResponseEntity.noContent().build();
    }

    private String currentUserLabel(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.email();
        }
        return "system";
    }
}
