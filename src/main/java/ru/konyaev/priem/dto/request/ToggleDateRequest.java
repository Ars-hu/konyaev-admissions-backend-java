package ru.konyaev.priem.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ToggleDateRequest {

    @NotBlank(message = "Дата обязательна")
    private String date;

    public ToggleDateRequest() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
