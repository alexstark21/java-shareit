package ru.practicum.shareit.item;

import lombok.Data;

@Data
public class ItemCreateDto {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private Long requestId;
}
