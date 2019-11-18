package org.example.data;

import io.vertx.core.json.JsonObject;

public class FileUploaded {

    private final String fileName;

    private final String contentType;

    private final long size;

    private final String uploadedFileName;


    public FileUploaded(String fileName, String contentType, long size, String uploadedFileName) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.uploadedFileName = uploadedFileName;
    }

    public FileUploaded(JsonObject fromJson) {
        fileName = fromJson.getString("fileName");
        contentType = fromJson.getString("contentType");
        size = fromJson.getLong("size");
        uploadedFileName = fromJson.getString("uploadedFileName");
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

    public String getUploadedFileName() {
        return uploadedFileName;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("fileName", fileName)
                .put("contentType", contentType)
                .put("size", size)
                .put("uploadedFileName", uploadedFileName);
    }
}
