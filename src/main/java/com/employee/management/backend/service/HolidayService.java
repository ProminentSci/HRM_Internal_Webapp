package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Holiday;
import com.employee.management.backend.dto.CreateHolidayDTO;
import com.employee.management.backend.dto.HolidayDTO;
import com.employee.management.backend.dto.UpdateHolidayDTO;
import com.employee.management.backend.exception.DuplicateResourceException;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public List<HolidayDTO> getHolidays(Long clientId, Integer year, LocalDate start, LocalDate end, Integer upcomingDays) {
        List<Holiday> holidays;
        if (upcomingDays != null && upcomingDays > 0) {
            LocalDate today = LocalDate.now();
            holidays = holidayRepository.findByClientIdAndDateBetweenOrderByDateAsc(clientId, today, today.plusDays(upcomingDays));
        } else if (start != null && end != null) {
            holidays = holidayRepository.findByClientIdAndDateBetweenOrderByDateAsc(clientId, start, end);
        } else if (year != null) {
            holidays = holidayRepository.findByYearAndClientId(year, clientId);
        } else {
            holidays = holidayRepository.findAllByClientIdOrderByDateAsc(clientId);
        }
        return holidays.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<Integer> getHolidayYears(Long clientId) {
        return holidayRepository.findDistinctYearsByClientId(clientId);
    }

    public HolidayDTO getHoliday(Long id, Long clientId) {
        Holiday holiday = holidayRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", "id", id));
        return convertToDto(holiday);
    }

    public HolidayDTO createHoliday(CreateHolidayDTO request, String createdBy, Long clientId) {
        ensureUniqueDateTitle(request.getDate(), request.getTitle(), null, clientId);
        Holiday holiday = new Holiday();
        holiday.setClientId(clientId);
        holiday.setDate(request.getDate());
        holiday.setTitle(request.getTitle().trim());
        holiday.setDescription(request.getDescription());
        holiday.setCreatedBy(createdBy);
        holiday.setCreatedAt(OffsetDateTime.now());
        Holiday saved = holidayRepository.save(holiday);
        return convertToDto(saved);
    }

    public HolidayDTO updateHoliday(Long id, UpdateHolidayDTO request, String updatedBy, Long clientId) {
        Holiday holiday = holidayRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", "id", id));

        LocalDate date = request.getDate() != null ? request.getDate() : holiday.getDate();
        String title = request.getTitle() != null ? request.getTitle().trim() : holiday.getTitle();
        ensureUniqueDateTitle(date, title, id, clientId);

        if (request.getDate() != null) {
            holiday.setDate(request.getDate());
        }
        if (request.getTitle() != null) {
            holiday.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            holiday.setDescription(request.getDescription());
        }

        holiday.setUpdatedBy(updatedBy);
        holiday.setUpdatedAt(OffsetDateTime.now());
        Holiday saved = holidayRepository.save(holiday);
        return convertToDto(saved);
    }

    public void deleteHoliday(Long id, Long clientId) {
        Holiday holiday = holidayRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", "id", id));
        holidayRepository.delete(holiday);
    }

    private void ensureUniqueDateTitle(LocalDate date, String title, Long currentId, Long clientId) {
        holidayRepository.findByDateAndTitleAndClientId(date, title, clientId).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new DuplicateResourceException("Holiday already exists for date and title");
            }
        });
    }

    private HolidayDTO convertToDto(Holiday holiday) {
        return new HolidayDTO(
                holiday.getId(),
                holiday.getDate(),
                holiday.getTitle(),
                holiday.getDescription(),
                holiday.getCreatedBy(),
                holiday.getCreatedAt(),
                holiday.getUpdatedBy(),
                holiday.getUpdatedAt()
        );
    }
}
