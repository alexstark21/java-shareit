package ru.practicum.shareit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.*;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceIntegrationTest {
    @Autowired
    private ItemService itemService;

    @PersistenceContext
    private EntityManager entityManager;

    private User owner;
    private User renter;

    @BeforeEach
    void setUp() {
        owner = IntegrationTestData.user(entityManager, "Owner", "owner@example.com");
        renter = IntegrationTestData.user(entityManager, "Renter", "renter@example.com");
    }

    @Test
    void shouldCreateItemWithOptionalRequestId() {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Drill");
        dto.setDescription("Cordless drill");
        dto.setAvailable(true);
        ItemRequest request = IntegrationTestData.request(entityManager, renter,
                "Need a drill", LocalDateTime.now());
        dto.setRequestId(request.getId());

        ItemDto created = itemService.createItem(owner.getId(), dto);

        assertNotNull(created.getId());
        assertEquals(request.getId(), created.getRequestId());
        entityManager.flush();
        entityManager.clear();
        Item saved = entityManager.find(Item.class, created.getId());
        assertEquals(owner.getId(), saved.getOwner().getId());
        assertEquals(request.getId(), saved.getRequestId());
    }

    @Test
    void shouldCreateItemWithoutRequestId() {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Hammer");
        dto.setDescription("Steel hammer");
        dto.setAvailable(true);

        ItemDto created = itemService.createItem(owner.getId(), dto);

        assertNull(created.getRequestId());
    }

    @Test
    void shouldUpdateItemFieldsForOwner() {
        Item item = IntegrationTestData.item(entityManager, owner, "Old", true);
        ItemUpdateDto update = new ItemUpdateDto();
        update.setName("New");
        update.setAvailable(false);

        ItemDto updated = itemService.updateItem(owner.getId(), item.getId(), update);

        assertEquals("New", updated.getName());
        assertFalse(updated.getAvailable());
        assertEquals(item.getDescription(), updated.getDescription());
    }

    @Test
    void shouldNotAllowAnotherUserToUpdateItem() {
        Item item = IntegrationTestData.item(entityManager, owner, "Owned", true);
        ItemUpdateDto update = new ItemUpdateDto();

        assertThrows(NotFoundException.class,
                () -> itemService.updateItem(renter.getId(), item.getId(), update));
    }

    @Test
    void shouldReturnItemWithCommentsAndBookingForOwner() {
        Item item = IntegrationTestData.item(entityManager, owner, "Camera", true);
        LocalDateTime now = LocalDateTime.now();
        IntegrationTestData.booking(entityManager, item, renter,
                now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED);
        IntegrationTestData.booking(entityManager, item, renter,
                now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED);
        IntegrationTestData.comment(entityManager, item, renter, "Good camera");

        ItemDto dto = itemService.getItemById(item.getId(), owner.getId());

        assertEquals("Camera", dto.getName());
        assertNotNull(dto.getLastBooking());
        assertNotNull(dto.getNextBooking());
        assertEquals(1, dto.getComments().size());
        assertEquals("Renter", dto.getComments().get(0).getAuthorName());
    }

    @Test
    void shouldHideBookingDetailsFromNonOwner() {
        Item item = IntegrationTestData.item(entityManager, owner, "Camera", true);
        LocalDateTime now = LocalDateTime.now();
        IntegrationTestData.booking(entityManager, item, renter,
                now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED);

        ItemDto dto = itemService.getItemById(item.getId(), renter.getId());

        assertNull(dto.getLastBooking());
        assertNull(dto.getNextBooking());
    }

    @Test
    void shouldReturnOwnerItemsWithCommentsAndBookingMetadata() {
        Item item = IntegrationTestData.item(entityManager, owner, "Laptop", true);
        LocalDateTime now = LocalDateTime.now();
        IntegrationTestData.booking(entityManager, item, renter,
                now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED);
        IntegrationTestData.booking(entityManager, item, renter,
                now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED);
        IntegrationTestData.comment(entityManager, item, renter, "Works well");

        Collection<ItemDto> items = itemService.getOwnerItems(owner.getId());

        assertEquals(1, items.size());
        ItemDto dto = items.iterator().next();
        assertNotNull(dto.getLastBooking());
        assertNotNull(dto.getNextBooking());
        assertEquals(1, dto.getComments().size());
    }

    @Test
    void shouldSearchOnlyAvailableItemsByNameOrDescription() {
        IntegrationTestData.item(entityManager, owner, "Blue bicycle", true);
        IntegrationTestData.item(entityManager, owner, "Red bicycle", false);
        IntegrationTestData.item(entityManager, owner, "Desk lamp", true);

        Collection<ItemDto> found = itemService.searchItems("BICYCLE");

        assertEquals(1, found.size());
        assertEquals("Blue bicycle", found.iterator().next().getName());
        assertTrue(itemService.searchItems("  ").isEmpty());
    }

    @Test
    void shouldAddCommentAfterCompletedApprovedBooking() {
        Item item = IntegrationTestData.item(entityManager, owner, "Table", true);
        IntegrationTestData.booking(entityManager, item, renter,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                BookingStatus.APPROVED);
        CommentCreateDto dto = new CommentCreateDto();
        dto.setText("Very useful");

        CommentDto comment = itemService.addComment(renter.getId(), item.getId(), dto);

        assertNotNull(comment.getId());
        assertEquals("Renter", comment.getAuthorName());
        assertEquals("Very useful", comment.getText());
    }

    @Test
    void shouldRejectCommentWithoutCompletedBooking() {
        Item item = IntegrationTestData.item(entityManager, owner, "Table", true);
        CommentCreateDto dto = new CommentCreateDto();
        dto.setText("Not rented");

        assertThrows(BadRequestException.class,
                () -> itemService.addComment(renter.getId(), item.getId(), dto));
    }

    @Test
    void shouldThrowWhenItemDoesNotExist() {
        assertThrows(NotFoundException.class, () -> itemService.getItemById(-1L, owner.getId()));
    }
}
