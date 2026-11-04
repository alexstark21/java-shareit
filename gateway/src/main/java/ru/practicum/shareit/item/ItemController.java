package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private static final String USER_HEADER = "X-Sharer-User-Id";
    private final ItemClient client;

    @PostMapping
    public Object create(@RequestHeader(USER_HEADER) Long userId,
                         @Valid @RequestBody ItemCreateDto dto) {
        return client.create(userId, dto);
    }

    @PatchMapping("/{itemId}")
    public Object update(@RequestHeader(USER_HEADER) Long userId,
                         @PathVariable Long itemId,
                         @Valid @RequestBody ItemUpdateDto dto) {
        return client.update(userId, itemId, dto);
    }

    @GetMapping("/{itemId}")
    public Object get(@RequestHeader(USER_HEADER) Long userId, @PathVariable Long itemId) {
        return client.get(userId, itemId);
    }

    @GetMapping
    public Object getByOwner(@RequestHeader(USER_HEADER) Long userId) {
        return client.getByOwner(userId);
    }

    @GetMapping("/search")
    public Object search(@RequestParam String text) {
        return client.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public Object addComment(@RequestHeader(USER_HEADER) Long userId,
                             @PathVariable Long itemId,
                             @Valid @RequestBody CommentCreateDto dto) {
        return client.addComment(userId, itemId, dto);
    }
}
