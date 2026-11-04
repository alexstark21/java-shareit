package ru.practicum.shareit.booking;

import java.util.Collection;

public interface BookingService {
    BookingDto create(Long userId, BookingCreateDto dto);

    BookingDto approve(Long userId, Long bookingId, boolean approved);

    BookingDto get(Long userId, Long bookingId);

    Collection<BookingDto> getByBooker(Long userId, BookingState state);

    Collection<BookingDto> getByOwner(Long userId, BookingState state);
}
