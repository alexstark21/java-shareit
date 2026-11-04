package ru.practicum.shareit.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemRequestCreateDto {
    @NotBlank(message = "Описание запроса не может быть пустым")
    @Size(max = 2000, message = "Описание запроса не должно превышать 2000 символов")
    private String description;
}
