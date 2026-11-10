package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DatesStatusResponse {

    @JsonProperty("blocked_dates")
    private List<String> blockedDates;

    @JsonProperty("full_dates")
    private List<String> fullDates;

    @JsonProperty("opened_weekends")
    private List<String> openedWeekends;

    @JsonProperty("blocked_within_24h")
    private List<String> blockedWithin24h;

    @JsonProperty("server_date")
    private String serverDate;

    @JsonProperty("server_now")
    private String serverNow;

    @JsonProperty("advance_hours")
    private Integer advanceHours;

    public DatesStatusResponse() {
    }

    public List<String> getBlockedDates() {
        return blockedDates;
    }

    public void setBlockedDates(List<String> blockedDates) {
        this.blockedDates = blockedDates;
    }

    public List<String> getFullDates() {
        return fullDates;
    }

    public void setFullDates(List<String> fullDates) {
        this.fullDates = fullDates;
    }

    public List<String> getOpenedWeekends() {
        return openedWeekends;
    }

    public void setOpenedWeekends(List<String> openedWeekends) {
        this.openedWeekends = openedWeekends;
    }

    public List<String> getBlockedWithin24h() {
        return blockedWithin24h;
    }

    public void setBlockedWithin24h(List<String> blockedWithin24h) {
        this.blockedWithin24h = blockedWithin24h;
    }

    public String getServerDate() {
        return serverDate;
    }

    public void setServerDate(String serverDate) {
        this.serverDate = serverDate;
    }

    public String getServerNow() {
        return serverNow;
    }

    public void setServerNow(String serverNow) {
        this.serverNow = serverNow;
    }

    public Integer getAdvanceHours() {
        return advanceHours;
    }

    public void setAdvanceHours(Integer advanceHours) {
        this.advanceHours = advanceHours;
    }
}
