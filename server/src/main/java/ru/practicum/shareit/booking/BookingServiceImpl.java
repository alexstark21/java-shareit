package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public BookingDto create(Long userId, BookingCreateDto dto) {
        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));
        if (item.getOwner().getId().equals(userId)) {
            throw new ConflictException("Нельзя бронировать собственную вещь");
        }
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new BadRequestException("Вещь недоступна для бронирования");
        }
        if (!dto.getStart().isBefore(dto.getEnd())) {
            throw new BadRequestException("Даты бронирования указаны некорректно");
        }
        if (bookingRepository.hasOverlap(item.getId(), dto.getStart(), dto.getEnd())) {
            throw new ConflictException("Вещь уже забронирована на эти даты");
        }
        Booking booking = BookingMapper.toBooking(dto, item, booker);
        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto approve(Long userId, Long bookingId, boolean approved) {
        Booking b = findBooking(bookingId);
        if (!b.getItem().getOwner().getId().equals(userId)) {
            throw new BadRequestException("Подтвердить бронирование может только владелец вещи");
        }
        if (b.getStatus() != BookingStatus.WAITING) {
            throw new ConflictException("Можно изменить только бронирование в статусе WAITING");
        }
        b.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toDto(bookingRepository.save(b));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto get(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndBookerIdOrItemOwnerId(bookingId, userId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено или у вас нет к нему доступа"));
        return BookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BookingDto> getByBooker(Long userId, BookingState state) {
        if (!userRepository.existsById(userId)) throw new NotFoundException("Пользователь не найден");
        return filter(bookingRepository.findAllByBookerIdOrderByStartDesc(userId), state);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BookingDto> getByOwner(Long userId, BookingState state) {
        if (!userRepository.existsById(userId)) throw new NotFoundException("Пользователь не найден");
        return filter(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(userId), state);
    }

    private Collection<BookingDto> filter(List<Booking> list, BookingState state) {
        LocalDateTime now = LocalDateTime.now();
        return list.stream().filter(b -> {
            switch (state == null ? BookingState.ALL : state) {
                case CURRENT:
                    return b.getStart().isBefore(now) && b.getEnd().isAfter(now);
                case PAST:
                    return !b.getEnd().isAfter(now);
                case FUTURE:
                    return b.getStart().isAfter(now);
                case WAITING:
                    return b.getStatus() == BookingStatus.WAITING;
                case REJECTED:
                    return b.getStatus() == BookingStatus.REJECTED;
                default:
                    return true;
            }
        }).map(BookingMapper::toDto).collect(Collectors.toList());
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));
    }
}
