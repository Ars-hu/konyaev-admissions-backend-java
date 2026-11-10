package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SlotsStatusResponse {

    @JsonProperty("date_blocked")
    private boolean dateBlocked;

    private Map<String, SlotInfoResponse> slots;

    @JsonProperty("daily_limit")
    private int dailyLimit;

    @JsonProperty("daily_count")
    private long dailyCount;

    public SlotsStatusResponse() {
    }

    public boolean isDateBlocked() {
        return dateBlocked;
    }

    public void setDateBlocked(boolean dateBlocked) {
        this.dateBlocked = dateBlocked;
    }

    public Map<String, SlotInfoResponse> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, SlotInfoResponse> slots) {
        this.slots = slots;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public long getDailyCount() {
        return dailyCount;
    }

    public void setDailyCount(long dailyCount) {
        this.dailyCount = dailyCount;
    }
}
