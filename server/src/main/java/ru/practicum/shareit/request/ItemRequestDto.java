package ru.practicum.shareit.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemRequestDto {
    private Long id;
    private String description;
    private Long requestorId;
    private LocalDateTime created;
    private List<ItemRequestItemDto> items;
}
