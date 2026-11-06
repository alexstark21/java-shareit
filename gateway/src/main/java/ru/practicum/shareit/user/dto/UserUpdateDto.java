package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDto {
    @Size(max = 255, message = "Имя пользователя слишком длинное")
    private String name;

    @Email(message = "Некорректный формат email")
    @Size(max = 512, message = "Email слишком длинный")
    private String email;
}
