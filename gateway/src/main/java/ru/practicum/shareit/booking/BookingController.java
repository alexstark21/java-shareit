package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingState;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private static final String USER_HEADER = "X-Sharer-User-Id";
    private final BookingClient client;

    @PostMapping
    public Object create(@RequestHeader(USER_HEADER) Long userId, @Valid @RequestBody BookingCreateDto dto) {
        if (dto.getStart() != null && dto.getEnd() != null && !dto.getStart().isBefore(dto.getEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Даты бронирования указаны некорректно");
        }
        return client.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public Object approve(@RequestHeader(USER_HEADER) Long userId,
                          @PathVariable Long bookingId,
                          @RequestParam boolean approved) {
        return client.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public Object get(@RequestHeader(USER_HEADER) Long userId, @PathVariable Long bookingId) {
        return client.get(userId, bookingId);
    }

    @GetMapping
    public Object byBooker(@RequestHeader(USER_HEADER) Long userId,
                           @RequestParam(defaultValue = "ALL") BookingState state) {
        return client.getByBooker(userId, state.name());
    }

    @GetMapping("/owner")
    public Object byOwner(@RequestHeader(USER_HEADER) Long userId,
                          @RequestParam(defaultValue = "ALL") BookingState state) {
        return client.getByOwner(userId, state.name());
    }
}
