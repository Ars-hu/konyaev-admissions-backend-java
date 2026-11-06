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
public class ApplicationResponse {
    private Long id;
    private String fio;
    private String phone;
    private String email;

    @JsonProperty("registration_date")
    private String registrationDate;

    @JsonProperty("registration_time")
    private String registrationTime;

    @JsonProperty("created_at")
    private String createdAt;

    private String status;
}
