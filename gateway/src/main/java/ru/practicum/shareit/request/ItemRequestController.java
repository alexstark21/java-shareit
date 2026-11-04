package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private static final String USER_HEADER = "X-Sharer-User-Id";
    private final ItemRequestClient client;

    @PostMapping
    public Object create(@RequestHeader(USER_HEADER) Long userId,
                         @Valid @RequestBody ItemRequestCreateDto dto) {
        return client.create(userId, dto);
    }

    @GetMapping
    public Object getOwn(@RequestHeader(USER_HEADER) Long userId) {
        return client.getOwn(userId);
    }

    @GetMapping("/all")
    public Object getAll(@RequestHeader(USER_HEADER) Long userId) {
        return client.getAll(userId);
    }

    @GetMapping("/{requestId}")
    public Object getById(@PathVariable Long requestId) {
        return client.getById(requestId);
    }
}
