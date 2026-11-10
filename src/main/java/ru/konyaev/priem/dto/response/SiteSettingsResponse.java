package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SiteSettingsResponse {

    @JsonProperty("advance_hours")
    private Integer advanceHours;

    public SiteSettingsResponse() {
    }

    public SiteSettingsResponse(Integer advanceHours) {
        this.advanceHours = advanceHours;
    }

    public Integer getAdvanceHours() {
        return advanceHours;
    }

    public void setAdvanceHours(Integer advanceHours) {
        this.advanceHours = advanceHours;
    }
}
