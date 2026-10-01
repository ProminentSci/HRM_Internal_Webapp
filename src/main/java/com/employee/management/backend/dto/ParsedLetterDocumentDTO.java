package com.employee.management.backend.dto;

import java.util.ArrayList;
import java.util.List;

// Preview-only result of parsing an uploaded .docx - never persisted directly. The admin reviews
// and adjusts these suggestions before anything is saved as a LetterTemplate.
public class ParsedLetterDocumentDTO {
    private List<String> paragraphs = new ArrayList<>();
    private List<SuggestedFieldDTO> suggestedFields = new ArrayList<>();

    public List<String> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<String> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<SuggestedFieldDTO> getSuggestedFields() {
        return suggestedFields;
    }

    public void setSuggestedFields(List<SuggestedFieldDTO> suggestedFields) {
        this.suggestedFields = suggestedFields;
    }

    public static class SuggestedFieldDTO {
        private String fieldKey;
        private String label;
        private String fieldType;
        private String matchedText;
        private int paragraphIndex;
        private int startOffset;
        private int endOffset;

        public SuggestedFieldDTO() {
        }

        public SuggestedFieldDTO(String fieldKey, String label, String fieldType, String matchedText,
                                  int paragraphIndex, int startOffset, int endOffset) {
            this.fieldKey = fieldKey;
            this.label = label;
            this.fieldType = fieldType;
            this.matchedText = matchedText;
            this.paragraphIndex = paragraphIndex;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        public String getFieldKey() {
            return fieldKey;
        }

        public void setFieldKey(String fieldKey) {
            this.fieldKey = fieldKey;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getFieldType() {
            return fieldType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public void setMatchedText(String matchedText) {
            this.matchedText = matchedText;
        }

        public int getParagraphIndex() {
            return paragraphIndex;
        }

        public void setParagraphIndex(int paragraphIndex) {
            this.paragraphIndex = paragraphIndex;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(int startOffset) {
            this.startOffset = startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public void setEndOffset(int endOffset) {
            this.endOffset = endOffset;
        }
    }
}
