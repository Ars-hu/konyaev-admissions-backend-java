package ru.konyaev.priem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
