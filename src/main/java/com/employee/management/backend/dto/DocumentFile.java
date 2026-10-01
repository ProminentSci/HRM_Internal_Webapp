package com.employee.management.backend.dto;

public class DocumentFile {
    private String fileName;
    private String contentType;
    private long size;
    private byte[] data;

    public DocumentFile() {
    }

    public DocumentFile(String fileName, String contentType, long size, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }
}
