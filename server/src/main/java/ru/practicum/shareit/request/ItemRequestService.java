package ru.practicum.shareit.request;

import java.util.Collection;

public interface ItemRequestService {
    ItemRequestDto create(Long userId, ItemRequestCreateDto dto);

    Collection<ItemRequestDto> getOwn(Long userId);

    Collection<ItemRequestDto> getAllOthers(Long userId);

    ItemRequestDto getById(Long requestId);
}
