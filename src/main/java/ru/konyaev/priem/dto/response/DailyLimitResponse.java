package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DailyLimitResponse {

    private String date;

    @JsonProperty("max_registrations")
    private Integer maxRegistrations;

    @JsonProperty("current_count")
    private Long currentCount;

    public DailyLimitResponse() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getMaxRegistrations() {
        return maxRegistrations;
    }

    public void setMaxRegistrations(Integer maxRegistrations) {
        this.maxRegistrations = maxRegistrations;
    }

    public Long getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(Long currentCount) {
        this.currentCount = currentCount;
    }
}
