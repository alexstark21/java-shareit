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
        if (dto.getStart() == null || dto.getEnd() == null ||
                !dto.getStart().isBefore(dto.getEnd()) || !dto.getStart().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Даты бронирования указаны некорректно");
        }
        Booking b = new Booking();
        b.setStart(dto.getStart());
        b.setEnd(dto.getEnd());
        b.setItem(item);
        b.setBooker(booker);
        b.setStatus(BookingStatus.WAITING);
        return BookingMapper.toDto(bookingRepository.save(b));
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
        Booking b = findBooking(bookingId);
        if (!b.getBooker().getId().equals(userId) && !b.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("Нет доступа к бронированию");
        }
        return BookingMapper.toDto(b);
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
