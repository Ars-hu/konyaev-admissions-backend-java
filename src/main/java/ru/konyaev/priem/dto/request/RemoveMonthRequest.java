package ru.konyaev.priem.dto.request;

import jakarta.validation.constraints.NotNull;

public class RemoveMonthRequest {

    @NotNull(message = "Год обязателен")
    private Integer year;

    @NotNull(message = "Месяц обязателен")
    private Integer month;

    public RemoveMonthRequest() {
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }
}
