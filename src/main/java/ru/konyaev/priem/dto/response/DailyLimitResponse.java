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
public class DailyLimitResponse {
    private String date;

    @JsonProperty("max_registrations")
    private Integer maxRegistrations;

    @JsonProperty("current_count")
    private Long currentCount;
}
