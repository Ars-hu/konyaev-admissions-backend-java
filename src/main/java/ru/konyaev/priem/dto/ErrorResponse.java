package ru.konyaev.priem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Единый формат JSON-ответа при любой ошибке (400, 401, 403, 404).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String error;
    private String message;
    private int status;
    private Instant timestamp;
    private Map<String, String> validationErrors;

    public ErrorResponse() {
    }

    public static ErrorResponse of(String message, int status) {
        ErrorResponse response = new ErrorResponse();
        response.setError(message);
        response.setMessage(message);
        response.setStatus(status);
        response.setTimestamp(Instant.now());
        return response;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
