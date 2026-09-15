package ru.practicum.shareit.item;

import java.util.Collection;
import java.util.Optional;

public interface ItemRepository {
    Item save(Item item);

    Item update(Item item);

    Optional<Item> findById(Long id);

    Collection<Item> findAllByOwnerId(Long ownerId);

    Collection<Item> search(String text);
}
