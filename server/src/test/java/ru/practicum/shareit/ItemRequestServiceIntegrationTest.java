package ru.practicum.shareit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.*;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceIntegrationTest {
    @Autowired
    private ItemRequestService requestService;

    @Autowired
    private ItemRequestRepository requestRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private User requestor;
    private User responder;
    private User viewer;

    @BeforeEach
    void setUp() {
        requestor = IntegrationTestData.user(userRepository, "Requestor", "requestor@example.com");
        responder = IntegrationTestData.user(userRepository, "Responder", "responder@example.com");
        viewer = IntegrationTestData.user(userRepository, "Viewer", "viewer@example.com");
    }

    @Test
    void shouldCreateRequestWithEmptyItems() {
        ItemRequestCreateDto dto = new ItemRequestCreateDto();
        dto.setDescription("Need a bicycle");

        ItemRequestDto created = requestService.create(requestor.getId(), dto);

        assertNotNull(created.getId());
        assertEquals("Need a bicycle", created.getDescription());
        assertEquals(requestor.getId(), created.getRequestorId());
        assertNotNull(created.getCreated());
        assertNotNull(created.getItems());
        assertTrue(created.getItems().isEmpty());
    }

    @Test
    void shouldReturnOwnRequestsNewestFirstWithResponseItems() {
        LocalDateTime now = LocalDateTime.now();
        ItemRequest older = IntegrationTestData.request(requestRepository, requestor,
                "Older request", now.minusDays(2));
        ItemRequest newer = IntegrationTestData.request(requestRepository, requestor,
                "Newer request", now.minusDays(1));
        IntegrationTestData.item(itemRepository, responder, "Bicycle", true, older.getId());
        IntegrationTestData.item(itemRepository, responder, "Helmet", true, newer.getId());

        List<ItemRequestDto> requests = List.copyOf(requestService.getOwn(requestor.getId()));

        assertEquals(2, requests.size());
        assertEquals(newer.getId(), requests.get(0).getId());
        assertEquals(older.getId(), requests.get(1).getId());
        assertEquals(1, requests.get(0).getItems().size());
        assertEquals("Helmet", requests.get(0).getItems().get(0).getName());
        assertEquals(responder.getId(), requests.get(0).getItems().get(0).getOwnerId());
        assertEquals(older.getId(), requests.get(1).getItems().get(0).getRequestId());
    }

    @Test
    void shouldReturnOnlyOtherUsersRequests() {
        LocalDateTime now = LocalDateTime.now();
        IntegrationTestData.request(requestRepository, requestor, "My request", now.minusDays(3));
        ItemRequest other = IntegrationTestData.request(requestRepository, responder,
                "Other request", now.minusDays(1));
        IntegrationTestData.request(requestRepository, viewer, "Another request", now.minusDays(2));

        Collection<ItemRequestDto> requests = requestService.getAllOthers(requestor.getId());

        assertEquals(2, requests.size());
        assertEquals(other.getId(), requests.iterator().next().getId());
        assertTrue(requests.stream().noneMatch(request -> request.getRequestorId().equals(requestor.getId())));
    }

    @Test
    void shouldReturnRequestByIdWithResponseItems() {
        ItemRequest request = IntegrationTestData.request(requestRepository, requestor,
                "Need a tent", LocalDateTime.now());
        IntegrationTestData.item(itemRepository, responder, "Tent", true, request.getId());

        ItemRequestDto found = requestService.getById(request.getId());

        assertEquals(request.getId(), found.getId());
        assertEquals("Need a tent", found.getDescription());
        assertEquals(1, found.getItems().size());
        assertEquals("Tent", found.getItems().get(0).getName());
    }

    @Test
    void shouldLoadResponsesWithoutNPlusOneQueries() {
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < 5; index++) {
            ItemRequest request = IntegrationTestData.request(requestRepository, requestor,
                    "Request " + index, now.minusMinutes(index));
            for (int itemIndex = 0; itemIndex < 3; itemIndex++) {
                IntegrationTestData.item(itemRepository, responder,
                        "Item " + index + "-" + itemIndex, true, request.getId());
            }
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        Collection<ItemRequestDto> requests = requestService.getOwn(requestor.getId());

        assertEquals(5, requests.size());
        assertTrue(statistics.getPrepareStatementCount() <= 3,
                "Запросы и вещи должны загружаться фиксированным числом SQL-запросов");
    }

    @Test
    void shouldThrowWhenRequestDoesNotExist() {
        assertThrows(NotFoundException.class, () -> requestService.getById(-1L));
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        assertThrows(NotFoundException.class, () -> requestService.getOwn(-1L));
        assertThrows(NotFoundException.class, () -> requestService.getAllOthers(-1L));
    }
}
