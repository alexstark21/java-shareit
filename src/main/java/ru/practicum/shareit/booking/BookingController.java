package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private static final String USER_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public BookingDto create(@RequestHeader(USER_HEADER) Long userId,
                             @Valid @RequestBody BookingCreateDto dto) {
        return bookingService.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approve(@RequestHeader(USER_HEADER) Long userId,
                              @PathVariable Long bookingId,
                              @RequestParam boolean approved) {
        return bookingService.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto get(@RequestHeader(USER_HEADER) Long userId, @PathVariable Long bookingId) {
        return bookingService.get(userId, bookingId);
    }

    @GetMapping
    public Collection<BookingDto> byBooker(@RequestHeader(USER_HEADER) Long userId,
                                           @RequestParam(defaultValue = "ALL") BookingState state) {
        return bookingService.getByBooker(userId, state);
    }

    @GetMapping("/owner")
    public Collection<BookingDto> byOwner(@RequestHeader(USER_HEADER) Long userId,
                                          @RequestParam(defaultValue = "ALL") BookingState state) {
        return bookingService.getByOwner(userId, state);
    }
}
