package ru.practicum.shareit.request;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemRequestDto create(Long userId, ItemRequestCreateDto dto) {
        User requestor = findUser(userId);
        ItemRequest request = ItemRequestMapper.toItemRequest(dto, requestor);
        return ItemRequestMapper.toDto(requestRepository.save(request), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ItemRequestDto> getOwn(Long userId) {
        findUser(userId);
        List<ItemRequest> requests = requestRepository.findAllByRequestorIdOrderByCreatedDesc(userId);
        return withItems(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ItemRequestDto> getAllOthers(Long userId) {
        findUser(userId);
        List<ItemRequest> requests = requestRepository.findAllByRequestorIdNotOrderByCreatedDesc(userId);
        return withItems(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemRequestDto getById(Long requestId) {
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id " + requestId + " не найден"));
        List<ItemRequest> oneRequest = List.of(request);
        return withItems(oneRequest).iterator().next();
    }

    private Collection<ItemRequestDto> withItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        Map<Long, List<ItemRequestItemDto>> itemsByRequest = itemRepository
                .findRequestItemsByRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(
                        ItemRequestItemDto::getRequestId,
                        LinkedHashMap::new,
                        Collectors.mapping(item -> new ItemRequestItemDto(
                                item.getId(),
                                item.getName(),
                                item.getOwnerId(),
                                item.getRequestId()
                        ), Collectors.toList())
                ));

        return requests.stream()
                .map(request -> ItemRequestMapper.toDto(
                        request, itemsByRequest.getOrDefault(request.getId(), List.of()))).collect(Collectors.toList());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }
}
