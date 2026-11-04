package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateDto {
    @NotBlank(message = "Текст комментария не может быть пустым")
    @Size(max = 2000)
    private String text;
}
