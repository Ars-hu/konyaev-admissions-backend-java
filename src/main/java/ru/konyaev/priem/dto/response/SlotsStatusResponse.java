package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotsStatusResponse {

    @JsonProperty("date_blocked")
    private boolean dateBlocked;

    private Map<String, SlotInfoResponse> slots;

    @JsonProperty("daily_limit")
    private int dailyLimit;

    @JsonProperty("daily_count")
    private long dailyCount;
}
