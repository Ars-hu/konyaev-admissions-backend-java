package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SlotInfoResponse {

    private int occupied;

    @JsonProperty("max_capacity")
    private int maxCapacity;

    @JsonProperty("is_blocked")
    private boolean isBlocked;

    public SlotInfoResponse() {
    }

    public int getOccupied() {
        return occupied;
    }

    public void setOccupied(int occupied) {
        this.occupied = occupied;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @JsonProperty("is_blocked")
    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}
