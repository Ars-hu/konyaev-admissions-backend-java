package ru.konyaev.priem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLimitRequest {
    @NotBlank(message = "Дата обязательна")
    @Size(max = 20, message = "Дата не может превышать 20 символов")
    private String date;

    @NotNull(message = "Лимит обязателен")
    @Min(value = 0, message = "Лимит не может быть отрицательным")
    @Max(value = 10000, message = "Лимит не может превышать 10000")
    @JsonProperty("max_registrations")
    private Integer maxRegistrations;
}
