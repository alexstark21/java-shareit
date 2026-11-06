package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final ItemRequestService requestService;

    @PostMapping
    public ItemRequestDto create(@RequestHeader(USER_ID_HEADER) Long userId,
                                 @RequestBody ItemRequestCreateDto dto) {
        return requestService.create(userId, dto);
    }

    @GetMapping
    public Collection<ItemRequestDto> getOwn(@RequestHeader(USER_ID_HEADER) Long userId) {
        return requestService.getOwn(userId);
    }

    @GetMapping("/all")
    public Collection<ItemRequestDto> getAll(@RequestHeader(USER_ID_HEADER) Long userId) {
        return requestService.getAllOthers(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getById(@PathVariable Long requestId) {
        return requestService.getById(requestId);
    }
}
