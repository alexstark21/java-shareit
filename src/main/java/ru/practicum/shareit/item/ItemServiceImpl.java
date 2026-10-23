package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final BookingRepository bookingRepository;

    @Override
    public ItemDto createItem(Long userId, ItemCreateDto itemCreateDto) {
        User owner = findUserOrThrow(userId);
        Item item = ItemMapper.toItem(itemCreateDto);
        item.setOwner(owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto updateItem(Long userId, Long itemId, ItemUpdateDto itemUpdateDto) {
        findUserOrThrow(userId);

        Item item = findItemOrThrow(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Редактировать вещь может только её владелец");
        }

        if (itemUpdateDto.getName() != null && !itemUpdateDto.getName().isBlank()) {
            item.setName(itemUpdateDto.getName());
        }
        if (itemUpdateDto.getDescription() != null && !itemUpdateDto.getDescription().isBlank()) {
            item.setDescription(itemUpdateDto.getDescription());
        }
        if (itemUpdateDto.getAvailable() != null) {
            item.setAvailable(itemUpdateDto.getAvailable());
        }

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemById(Long itemId) {
        Item item = findItemOrThrow(itemId);
        ItemDto dto = ItemMapper.toItemDto(item);
        dto.setComments(getComments(itemId));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ItemDto> getOwnerItems(Long userId) {
        findUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Item> items = itemRepository.findAllByOwnerIdOrderById(userId);
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());

        List<Booking> allBookings = bookingRepository.findAllByItemIdInAndStatus(itemIds, BookingStatus.APPROVED);
        Map<Long, List<Booking>> bookingsByItem = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));

        List<Comment> allComments = commentRepository.findAllByItemIdIn(itemIds);
        Map<Long, List<Comment>> commentsByItem = allComments.stream()
                .collect(Collectors.groupingBy(c -> c.getItem().getId()));

        return items.stream().map(item -> {
            ItemDto dto = ItemMapper.toItemDto(item);

            List<Booking> approved = bookingsByItem.getOrDefault(item.getId(), Collections.emptyList());

            Booking last = approved.stream().filter(b -> !b.getEnd().isAfter(now))
                    .max(Comparator.comparing(Booking::getEnd)).orElse(null);
            Booking next = approved.stream().filter(b -> b.getStart().isAfter(now))
                    .min(Comparator.comparing(Booking::getStart)).orElse(null);

            dto.setLastBooking(last == null ? null : BookingMapper.toDto(last));
            dto.setNextBooking(next == null ? null : BookingMapper.toDto(next));

            List<CommentDto> comments = commentsByItem.getOrDefault(item.getId(), Collections.emptyList()).stream()
                    .map(CommentMapper::toCommentDto)
                    .collect(Collectors.toList());
            dto.setComments(comments);

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto dto) {
        User author = findUserOrThrow(userId);
        Item item = findItemOrThrow(itemId);
        boolean rented = bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndBefore(
                userId, itemId, BookingStatus.APPROVED, LocalDateTime.now());

        if (!rented) {
            throw new BadRequestException("Оставить отзыв можно только после завершённого бронирования");
        }

        Comment comment = CommentMapper.toComment(dto, item, author);
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private List<CommentDto> getComments(Long itemId) {
        return commentRepository.findAllByItemIdOrderByCreatedDesc(itemId).stream()
                .map(CommentMapper::toCommentDto).collect(Collectors.toList());
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
    }
}
