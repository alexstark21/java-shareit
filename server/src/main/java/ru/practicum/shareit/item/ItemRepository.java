package ru.practicum.shareit.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.request.ItemRequestItemDto;

import java.util.Collection;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findAllByOwnerIdOrderById(Long ownerId);

    @Query("SELECT new ru.practicum.shareit.request.ItemRequestItemDto(i.id, i.name, i.owner.id, i.requestId) " +
            "FROM Item i WHERE i.requestId IN :requestIds")
    List<ItemRequestItemDto> findRequestItemsByRequestIdIn(@Param("requestIds") List<Long> requestIds);

    @Query("select i from Item i where i.available = true and " +
            "(lower(i.name) like lower(concat('%', :text, '%')) or " +
            "lower(i.description) like lower(concat('%', :text, '%')))")
    Collection<Item> search(@Param("text") String text);
}
