package ru.konyaev.priem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequest {
    @Min(value = 0, message = "Часы не могут быть отрицательными")
    @Max(value = 720, message = "Значение не может превышать 720 часов")
    @JsonProperty("advance_hours")
    private Integer advanceHours;
}
