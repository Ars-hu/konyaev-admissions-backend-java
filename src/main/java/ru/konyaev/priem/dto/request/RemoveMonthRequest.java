package ru.konyaev.priem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveMonthRequest {
    @NotNull(message = "Год обязателен")
    private Integer year;

    @NotNull(message = "Месяц обязателен")
    private Integer month;
}
