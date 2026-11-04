package ru.practicum.shareit.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequestItemDto {
    private Long id;
    private String name;
    private Long ownerId;
    private Long requestId;
}
