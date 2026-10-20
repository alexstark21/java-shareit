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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

        return itemRepository.findAllByOwnerIdOrderById(userId).stream().map(item -> {
            ItemDto dto = ItemMapper.toItemDto(item);
            List<Booking> approved = bookingRepository.findAllByItemIdAndStatus(item.getId(), BookingStatus.APPROVED);
            Booking last = approved.stream().filter(b -> !b.getEnd().isAfter(now))
                    .max(Comparator.comparing(Booking::getEnd)).orElse(null);
            Booking next = approved.stream().filter(b -> b.getStart().isAfter(now))
                    .min(Comparator.comparing(Booking::getStart)).orElse(null);
            dto.setLastBooking(last == null ? null : BookingMapper.toDto(last));
            dto.setNextBooking(next == null ? null : BookingMapper.toDto(next));
            dto.setComments(getComments(item.getId()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
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

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setAuthor(author);
        comment.setItem(item);
        comment.setCreated(LocalDateTime.now());
        return toCommentDto(commentRepository.save(comment));
    }

    private List<CommentDto> getComments(Long itemId) {
        return commentRepository.findAllByItemIdOrderByCreatedDesc(itemId).stream()
                .map(this::toCommentDto).collect(Collectors.toList());
    }

    private CommentDto toCommentDto(Comment c) {
        CommentDto dto = new CommentDto();
        dto.setId(c.getId());
        dto.setText(c.getText());
        dto.setAuthorName(c.getAuthor().getName());
        dto.setCreated(c.getCreated());
        return dto;
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
