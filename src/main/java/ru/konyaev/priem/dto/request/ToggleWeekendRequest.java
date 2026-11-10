package ru.konyaev.priem.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ToggleWeekendRequest {

    @NotBlank(message = "Дата обязательна")
    private String date;

    public ToggleWeekendRequest() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
