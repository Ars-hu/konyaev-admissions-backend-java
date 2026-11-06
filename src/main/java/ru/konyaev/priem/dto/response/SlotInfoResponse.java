package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotInfoResponse {

    private int occupied;

    @JsonProperty("max_capacity")
    private int maxCapacity;

    @JsonProperty("is_blocked")
    private boolean isBlocked;
}
