package ru.practicum.shareit.item;

import java.util.Collection;

public interface ItemService {
    ItemDto createItem(Long userId, ItemDto itemDto);

    ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto);

    ItemDto getItemById(Long itemId);

    Collection<ItemDto> getOwnerItems(Long userId);

    Collection<ItemDto> searchItems(String text);
}
