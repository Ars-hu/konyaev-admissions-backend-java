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
public class ArchivedApplicationResponse {
    private Long id;

    @JsonProperty("original_id")
    private Long originalId;

    private String fio;
    private String phone;
    private String email;

    @JsonProperty("registration_date")
    private String registrationDate;

    @JsonProperty("registration_time")
    private String registrationTime;

    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("archived_at")
    private String archivedAt;
}
