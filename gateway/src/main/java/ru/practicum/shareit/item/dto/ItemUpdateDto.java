package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemUpdateDto {
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private Boolean available;
}
