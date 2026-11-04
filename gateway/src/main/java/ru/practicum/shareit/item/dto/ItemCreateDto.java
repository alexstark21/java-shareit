package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemCreateDto {
    @NotBlank(message = "Название вещи не может быть пустым")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Описание вещи не может быть пустым")
    @Size(max = 2000)
    private String description;

    @NotNull(message = "Статус доступности вещи должен быть указан")
    private Boolean available;

    @Positive(message = "requestId должен быть положительным")
    private Long requestId;
}
