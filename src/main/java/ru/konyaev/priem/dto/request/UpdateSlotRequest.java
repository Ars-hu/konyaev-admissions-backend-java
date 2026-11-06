package ru.konyaev.priem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSlotRequest {

    @NotBlank(message = "Дата обязательна")
    @Size(max = 20, message = "Дата не может превышать 20 символов")
    private String date;

    @NotBlank(message = "Время обязательно")
    @Size(max = 10, message = "Время не может превышать 10 символов")
    private String time;

    @Min(value = 1, message = "Вместимость должна быть не менее 1")
    @Max(value = 100, message = "Вместимость не может превышать 100")
    @JsonProperty("max_capacity")
    private Integer maxCapacity;

    @JsonProperty("is_blocked")
    private Boolean isBlocked;
}
