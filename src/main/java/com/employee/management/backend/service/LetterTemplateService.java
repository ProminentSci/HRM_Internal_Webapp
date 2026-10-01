package com.employee.management.backend.service;

import com.employee.management.backend.Entity.LetterTemplate;
import com.employee.management.backend.Entity.TemplateField;
import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.dto.LetterTemplateDTO;
import com.employee.management.backend.dto.ParsedLetterDocumentDTO;
import com.employee.management.backend.dto.TemplateFieldDTO;
import com.employee.management.backend.repository.LetterTemplateRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LetterTemplateService {

    // Must match the routes under /admin/essentials on the frontend.
    private static final Set<String> LETTER_TYPES = Set.of("OFFER", "LETTER_OF_INTENT", "EXPERIENCE", "RELIEVING");

    // Best-effort candidate detectors for the "auto-detect + admin review" upload flow - the admin
    // confirms/renames/removes every suggestion before anything is saved, so false positives here
    // are cheap; false negatives just mean the admin marks that span manually instead.
    private static final Pattern DATE_WORDS_PATTERN = Pattern.compile(
            "\\d{1,2}(st|nd|rd|th)?\\s+(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-zA-Z]*\\s+\\d{4}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_NUMERIC_PATTERN = Pattern.compile("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("(Rs\\.?|INR|\\u20B9)\\s?[\\d,]+(\\.\\d+)?");
    private static final Pattern DEAR_NAME_PATTERN = Pattern.compile("(?:Dear|Mr/Ms\\.?|Mr\\.|Ms\\.|Mrs\\.)\\s+([A-Z][a-zA-Z.]*(?:\\s+[A-Z][a-zA-Z.]*){0,3})");
    private static final Pattern DESIGNATION_PATTERN = Pattern.compile("as\\s+(?:a|an|the)\\s+([A-Za-z][A-Za-z /]{2,40}?)(?=[,.\\n]|$)");

    private final LetterTemplateRepository letterTemplateRepository;

    public LetterTemplateService(LetterTemplateRepository letterTemplateRepository) {
        this.letterTemplateRepository = letterTemplateRepository;
    }

    // Preview-only: reads the uploaded .docx and returns candidate paragraphs/fields for the admin
    // to review. Nothing is persisted here - see createTemplate/updateTemplate for that.
    public ParsedLetterDocumentDTO parseDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("A .docx file is required");
        }

        List<String> paragraphs = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    paragraphs.add(text.trim());
                }
            }
        } catch (IOException | RuntimeException ex) {
            throw new RuntimeException("Failed to read the uploaded document - make sure it's a valid .docx file");
        }

        List<ParsedLetterDocumentDTO.SuggestedFieldDTO> suggestions = new ArrayList<>();
        Map<String, Integer> keyCounters = new HashMap<>();
        for (int paragraphIndex = 0; paragraphIndex < paragraphs.size(); paragraphIndex++) {
            String text = paragraphs.get(paragraphIndex);
            detectMatches(text, DEAR_NAME_PATTERN, 1, "employeeName", "Employee Name", "TEXT", paragraphIndex, keyCounters, suggestions);
            detectMatches(text, DESIGNATION_PATTERN, 1, "designation", "Designation", "TEXT", paragraphIndex, keyCounters, suggestions);
            detectMatches(text, DATE_WORDS_PATTERN, 0, "date", "Date", "DATE", paragraphIndex, keyCounters, suggestions);
            detectMatches(text, DATE_NUMERIC_PATTERN, 0, "date", "Date", "DATE", paragraphIndex, keyCounters, suggestions);
            detectMatches(text, CURRENCY_PATTERN, 0, "amount", "Amount", "CURRENCY", paragraphIndex, keyCounters, suggestions);
        }

        ParsedLetterDocumentDTO result = new ParsedLetterDocumentDTO();
        result.setParagraphs(paragraphs);
        result.setSuggestedFields(suggestions);
        return result;
    }

    private void detectMatches(String text, Pattern pattern, int group, String baseKey, String baseLabel, String fieldType,
                                int paragraphIndex, Map<String, Integer> keyCounters,
                                List<ParsedLetterDocumentDTO.SuggestedFieldDTO> suggestions) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedText = matcher.group(group);
            if (matchedText == null || matchedText.trim().isEmpty()) {
                continue;
            }
            int start = matcher.start(group);
            int end = matcher.end(group);

            int count = keyCounters.merge(baseKey, 1, Integer::sum);
            String fieldKey = count == 1 ? baseKey : baseKey + count;
            String label = count == 1 ? baseLabel : baseLabel + " " + count;

            suggestions.add(new ParsedLetterDocumentDTO.SuggestedFieldDTO(
                    fieldKey, label, fieldType, matchedText.trim(), paragraphIndex, start, end));
        }
    }

    @Transactional(readOnly = true)
    public List<LetterTemplateDTO> listTemplates(Long clientId, String letterType) {
        String normalized = normalizeLetterType(letterType);
        return letterTemplateRepository.findByClientIdAndLetterTypeOrderByNameAsc(clientId, normalized).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LetterTemplateDTO createTemplate(Long clientId, LetterTemplateDTO dto) {
        LetterTemplate template = new LetterTemplate();
        template.setClientId(clientId);
        template.setLetterType(normalizeLetterType(dto.getLetterType()));
        applyFields(template, dto);

        if (template.isDefault()) {
            clearOtherDefaults(clientId, template.getLetterType(), null);
        }

        return toDto(letterTemplateRepository.save(template));
    }

    @Transactional
    public LetterTemplateDTO updateTemplate(Long id, Long clientId, LetterTemplateDTO dto) {
        LetterTemplate template = letterTemplateRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new RuntimeException("Letter template not found"));
        applyFields(template, dto);

        if (template.isDefault()) {
            clearOtherDefaults(clientId, template.getLetterType(), id);
        }

        return toDto(letterTemplateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long id, Long clientId) {
        LetterTemplate template = letterTemplateRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new RuntimeException("Letter template not found"));
        letterTemplateRepository.delete(template);
    }

    @Transactional
    public void uploadLogo(Long id, Long clientId, MultipartFile file) {
        LetterTemplate template = letterTemplateRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new RuntimeException("Letter template not found"));
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("A logo image file is required");
        }
        try {
            template.setLogoData(file.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read uploaded logo file");
        }
        template.setLogoFileName(file.getOriginalFilename());
        template.setLogoContentType(file.getContentType());
        letterTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public DocumentFile getLogo(Long id, Long clientId) {
        LetterTemplate template = letterTemplateRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new RuntimeException("Letter template not found"));
        if (template.getLogoData() == null) {
            throw new RuntimeException("No logo has been uploaded for this template");
        }
        return new DocumentFile(template.getLogoFileName(), template.getLogoContentType(),
                template.getLogoData().length, template.getLogoData());
    }

    @Transactional
    public void uploadSignature(Long id, Long clientId, MultipartFile file) {
        LetterTemplate template = letterTemplateRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new RuntimeException("Letter template not found"));
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("A signature image file is required");
        }
        try {
            template.setSignatureData(file.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read uploaded signature file");
        }
        template.setSignatureFileName(file.getOriginalFilename());
        template.setSignatureContentType(file.getContentType());
        letterTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public DocumentFile getSignature(Long id, Long clientId) {
        LetterTemplate template = letterTemplateRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new RuntimeException("Letter template not found"));
        if (template.getSignatureData() == null) {
            throw new RuntimeException("No signature has been uploaded for this template");
        }
        return new DocumentFile(template.getSignatureFileName(), template.getSignatureContentType(),
                template.getSignatureData().length, template.getSignatureData());
    }

    private void applyFields(LetterTemplate template, LetterTemplateDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Template name is required");
        }
        template.setName(dto.getName().trim());
        template.setCompanyName(dto.getCompanyName());
        template.setAddressLine1(dto.getAddressLine1());
        template.setAddressLine2(dto.getAddressLine2());
        template.setAddressLine3(dto.getAddressLine3());
        template.setPhone(dto.getPhone());
        template.setWebsite(dto.getWebsite());
        template.setEmail(dto.getEmail());
        template.setHrName(dto.getHrName());
        template.setDefault(dto.isDefault());
        template.setBodyContent(dto.getBodyContent());

        List<TemplateField> fields = dto.getFields() == null ? List.of() : dto.getFields().stream()
                .map(f -> new TemplateField(f.getFieldKey(), f.getLabel(), f.getFieldType()))
                .collect(Collectors.toList());
        template.getFields().clear();
        template.getFields().addAll(fields);
    }

    // Only one default per (client, letterType) - saving a new default here clears the flag on
    // every other template in that same group rather than requiring the caller to do it.
    private void clearOtherDefaults(Long clientId, String letterType, Long exceptId) {
        List<LetterTemplate> siblings = letterTemplateRepository.findByClientIdAndLetterTypeOrderByNameAsc(clientId, letterType);
        for (LetterTemplate sibling : siblings) {
            if (sibling.isDefault() && !sibling.getId().equals(exceptId)) {
                sibling.setDefault(false);
                letterTemplateRepository.save(sibling);
            }
        }
    }

    private String normalizeLetterType(String letterType) {
        String normalized = letterType == null ? "" : letterType.trim().toUpperCase();
        if (!LETTER_TYPES.contains(normalized)) {
            throw new RuntimeException("Invalid letter type: " + letterType);
        }
        return normalized;
    }

    private LetterTemplateDTO toDto(LetterTemplate template) {
        LetterTemplateDTO dto = new LetterTemplateDTO();
        dto.setId(template.getId());
        dto.setLetterType(template.getLetterType());
        dto.setName(template.getName());
        dto.setCompanyName(template.getCompanyName());
        dto.setAddressLine1(template.getAddressLine1());
        dto.setAddressLine2(template.getAddressLine2());
        dto.setAddressLine3(template.getAddressLine3());
        dto.setPhone(template.getPhone());
        dto.setWebsite(template.getWebsite());
        dto.setEmail(template.getEmail());
        dto.setHrName(template.getHrName());
        dto.setDefault(template.isDefault());
        dto.setHasLogo(template.getLogoData() != null);
        dto.setHasSignature(template.getSignatureData() != null);
        dto.setBodyContent(template.getBodyContent());
        dto.setFields(template.getFields().stream()
                .map(f -> new TemplateFieldDTO(f.getFieldKey(), f.getLabel(), f.getFieldType()))
                .collect(Collectors.toList()));
        return dto;
    }
}
