package com.employee.management.backend.service;

import com.employee.management.backend.Entity.PayslipLineItem;
import com.employee.management.backend.Entity.PayslipSettings;
import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.dto.PayslipLineItemDTO;
import com.employee.management.backend.dto.PayslipSettingsDTO;
import com.employee.management.backend.repository.PayslipSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayslipSettingsService {

    private static final double PERCENT_SUM_TOLERANCE = 0.01;

    private final PayslipSettingsRepository payslipSettingsRepository;

    public PayslipSettingsService(PayslipSettingsRepository payslipSettingsRepository) {
        this.payslipSettingsRepository = payslipSettingsRepository;
    }

    @Transactional(readOnly = true)
    public PayslipSettingsDTO getSettings(Long clientId) {
        return toDto(findOrDefault(clientId));
    }

    @Transactional
    public PayslipSettingsDTO updateSettings(Long clientId, PayslipSettingsDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        double basicPercent = valueOrDefault(dto.getBasicPercent(), 50.0);
        double hraPercent = valueOrDefault(dto.getHraPercent(), 20.0);
        double specialAllowancePercent = valueOrDefault(dto.getSpecialAllowancePercent(), 30.0);

        List<PayslipLineItem> customEarnings = toLineItems(dto.getCustomEarnings());
        List<PayslipLineItem> customDeductions = toLineItems(dto.getCustomDeductions());

        // Percent-type custom earnings share the same 100% split as Basic/HRA/Special (Flat ₹
        // rows are added on top and don't count here) - so a client can carve out part of the
        // fixed split for a custom earning instead of being forced to keep basic+hra+special at 100.
        double customEarningsPercent = customEarnings.stream()
                .filter(item -> "PERCENT".equals(item.getValueType()))
                .mapToDouble(PayslipLineItem::getValue)
                .sum();
        double total = basicPercent + hraPercent + specialAllowancePercent + customEarningsPercent;
        if (Math.abs(total - 100.0) > PERCENT_SUM_TOLERANCE) {
            throw new RuntimeException("Basic %, HRA %, Special Allowance % and percent-based Custom Earnings must add up to 100");
        }

        PayslipSettings settings = payslipSettingsRepository.findByClientId(clientId).orElseGet(() -> {
            PayslipSettings created = new PayslipSettings();
            created.setClientId(clientId);
            return created;
        });

        settings.setCompanyName(dto.getCompanyName());
        settings.setAddressLine1(dto.getAddressLine1());
        settings.setAddressLine2(dto.getAddressLine2());
        settings.setAddressLine3(dto.getAddressLine3());
        settings.setPhone(dto.getPhone());
        settings.setWebsite(dto.getWebsite());
        settings.setEmail(dto.getEmail());
        settings.setBasicPercent(basicPercent);
        settings.setHraPercent(hraPercent);
        settings.setSpecialAllowancePercent(specialAllowancePercent);
        settings.setPfPercent(valueOrDefault(dto.getPfPercent(), 12.0));
        settings.setProfessionalTax(valueOrDefault(dto.getProfessionalTax(), 200.0));
        settings.setEsiAmount(valueOrDefault(dto.getEsiAmount(), 600.0));

        settings.getCustomEarnings().clear();
        settings.getCustomEarnings().addAll(customEarnings);
        settings.getCustomDeductions().clear();
        settings.getCustomDeductions().addAll(customDeductions);

        return toDto(payslipSettingsRepository.save(settings));
    }

    @Transactional
    public void uploadLogo(Long clientId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("A logo image file is required");
        }

        PayslipSettings settings = payslipSettingsRepository.findByClientId(clientId).orElseGet(() -> {
            PayslipSettings created = new PayslipSettings();
            created.setClientId(clientId);
            return created;
        });

        try {
            settings.setLogoData(file.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read uploaded logo file");
        }
        settings.setLogoFileName(file.getOriginalFilename());
        settings.setLogoContentType(file.getContentType());

        payslipSettingsRepository.save(settings);
    }

    @Transactional(readOnly = true)
    public DocumentFile getLogo(Long clientId) {
        PayslipSettings settings = payslipSettingsRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("No payslip logo has been uploaded"));

        if (settings.getLogoData() == null) {
            throw new RuntimeException("No payslip logo has been uploaded");
        }

        return new DocumentFile(
                settings.getLogoFileName(),
                settings.getLogoContentType(),
                settings.getLogoData().length,
                settings.getLogoData()
        );
    }

    private List<PayslipLineItem> toLineItems(List<PayslipLineItemDTO> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream().map(this::toLineItem).collect(Collectors.toList());
    }

    private PayslipLineItem toLineItem(PayslipLineItemDTO row) {
        if (row == null || row.getLabel() == null || row.getLabel().trim().isEmpty()) {
            throw new RuntimeException("Each custom earning/deduction needs a label");
        }
        String valueType = row.getValueType() == null ? "" : row.getValueType().trim().toUpperCase();
        if (!valueType.equals("FLAT") && !valueType.equals("PERCENT")) {
            throw new RuntimeException("Custom item \"" + row.getLabel() + "\" must be FLAT or PERCENT");
        }
        double value = row.getValue() == null ? 0 : row.getValue();
        if (value < 0) {
            throw new RuntimeException("Custom item \"" + row.getLabel() + "\" cannot have a negative value");
        }
        return new PayslipLineItem(row.getLabel().trim(), valueType, value);
    }

    private List<PayslipLineItemDTO> toLineItemDtos(List<PayslipLineItem> items) {
        return items.stream()
                .map(item -> new PayslipLineItemDTO(item.getLabel(), item.getValueType(), item.getValue()))
                .collect(Collectors.toList());
    }

    private PayslipSettings findOrDefault(Long clientId) {
        return payslipSettingsRepository.findByClientId(clientId).orElseGet(() -> {
            PayslipSettings defaults = new PayslipSettings();
            defaults.setClientId(clientId);
            return defaults;
        });
    }

    private PayslipSettingsDTO toDto(PayslipSettings settings) {
        PayslipSettingsDTO dto = new PayslipSettingsDTO();
        dto.setCompanyName(settings.getCompanyName());
        dto.setAddressLine1(settings.getAddressLine1());
        dto.setAddressLine2(settings.getAddressLine2());
        dto.setAddressLine3(settings.getAddressLine3());
        dto.setPhone(settings.getPhone());
        dto.setWebsite(settings.getWebsite());
        dto.setEmail(settings.getEmail());
        dto.setBasicPercent(settings.getBasicPercent());
        dto.setHraPercent(settings.getHraPercent());
        dto.setSpecialAllowancePercent(settings.getSpecialAllowancePercent());
        dto.setPfPercent(settings.getPfPercent());
        dto.setProfessionalTax(settings.getProfessionalTax());
        dto.setEsiAmount(settings.getEsiAmount());
        dto.setHasLogo(settings.getLogoData() != null);
        dto.setCustomEarnings(toLineItemDtos(settings.getCustomEarnings()));
        dto.setCustomDeductions(toLineItemDtos(settings.getCustomDeductions()));
        return dto;
    }

    private double valueOrDefault(Double value, double fallback) {
        return value != null ? value : fallback;
    }
}
