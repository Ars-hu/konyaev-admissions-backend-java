package ru.konyaev.priem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterApplicationRequest {

    @NotBlank(message = "ФИО обязательно для заполнения")
    @Size(max = 255, message = "ФИО не может быть длиннее 255 символов")
    private String fio;

    @NotBlank(message = "Номер телефона обязателен для заполнения")
    @Size(max = 20, message = "Номер телефона не может превышать 20 символов")
    private String phone;

    @NotBlank(message = "Email обязателен для заполнения")
    @Email(message = "Некорректный формат email")
    @Size(max = 100, message = "Email не может превышать 100 символов")
    private String email;

    @NotBlank(message = "Дата записи обязательна")
    @Size(max = 20, message = "Дата не может превышать 20 символов")
    @JsonProperty("registration_date")
    private String registrationDate;

    @NotBlank(message = "Время записи обязательно")
    @Size(max = 10, message = "Время не может превышать 10 символов")
    @JsonProperty("registration_time")
    private String registrationTime;

    public RegisterApplicationRequest() {
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(String registrationTime) {
        this.registrationTime = registrationTime;
    }
}
