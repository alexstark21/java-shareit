package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.*;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceIntegrationTest {
    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User owner;
    private User renter;

    @BeforeEach
    void setUp() {
        owner = IntegrationTestData.user(userRepository, "Owner", "owner@example.com");
        renter = IntegrationTestData.user(userRepository, "Renter", "renter@example.com");
    }

    @Test
    void shouldCreateItemWithOptionalRequestId() {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Drill");
        dto.setDescription("Cordless drill");
        dto.setAvailable(true);
        dto.setRequestId(42L);

        ItemDto created = itemService.createItem(owner.getId(), dto);

        assertNotNull(created.getId());
        assertEquals(42L, created.getRequestId());
        assertEquals(owner.getId(), itemRepository.findById(created.getId()).orElseThrow().getOwner().getId());
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
        Item item = IntegrationTestData.item(itemRepository, owner, "Old", true);
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
        Item item = IntegrationTestData.item(itemRepository, owner, "Owned", true);
        ItemUpdateDto update = new ItemUpdateDto();

        assertThrows(NotFoundException.class,
                () -> itemService.updateItem(renter.getId(), item.getId(), update));
    }

    @Test
    void shouldReturnItemWithCommentsAndBookingForOwner() {
        Item item = IntegrationTestData.item(itemRepository, owner, "Camera", true);
        LocalDateTime now = LocalDateTime.now();
        IntegrationTestData.booking(bookingRepository, item, renter,
                now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED);
        IntegrationTestData.booking(bookingRepository, item, renter,
                now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED);
        IntegrationTestData.comment(commentRepository, item, renter, "Good camera");

        ItemDto dto = itemService.getItemById(item.getId(), owner.getId());

        assertEquals("Camera", dto.getName());
        assertNotNull(dto.getLastBooking());
        assertNotNull(dto.getNextBooking());
        assertEquals(1, dto.getComments().size());
        assertEquals("Renter", dto.getComments().get(0).getAuthorName());
    }

    @Test
    void shouldHideBookingDetailsFromNonOwner() {
        Item item = IntegrationTestData.item(itemRepository, owner, "Camera", true);
        LocalDateTime now = LocalDateTime.now();
        IntegrationTestData.booking(bookingRepository, item, renter,
                now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED);

        ItemDto dto = itemService.getItemById(item.getId(), renter.getId());

        assertNull(dto.getLastBooking());
        assertNull(dto.getNextBooking());
    }

    @Test
    void shouldReturnOwnerItemsWithCommentsAndBookingMetadata() {
        Item item = IntegrationTestData.item(itemRepository, owner, "Laptop", true);
        LocalDateTime now = LocalDateTime.now();
        IntegrationTestData.booking(bookingRepository, item, renter,
                now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED);
        IntegrationTestData.booking(bookingRepository, item, renter,
                now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED);
        IntegrationTestData.comment(commentRepository, item, renter, "Works well");

        Collection<ItemDto> items = itemService.getOwnerItems(owner.getId());

        assertEquals(1, items.size());
        ItemDto dto = items.iterator().next();
        assertNotNull(dto.getLastBooking());
        assertNotNull(dto.getNextBooking());
        assertEquals(1, dto.getComments().size());
    }

    @Test
    void shouldSearchOnlyAvailableItemsByNameOrDescription() {
        IntegrationTestData.item(itemRepository, owner, "Blue bicycle", true);
        IntegrationTestData.item(itemRepository, owner, "Red bicycle", false);
        IntegrationTestData.item(itemRepository, owner, "Desk lamp", true);

        Collection<ItemDto> found = itemService.searchItems("BICYCLE");

        assertEquals(1, found.size());
        assertEquals("Blue bicycle", found.iterator().next().getName());
        assertTrue(itemService.searchItems("  ").isEmpty());
    }

    @Test
    void shouldAddCommentAfterCompletedApprovedBooking() {
        Item item = IntegrationTestData.item(itemRepository, owner, "Table", true);
        IntegrationTestData.booking(bookingRepository, item, renter,
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
        Item item = IntegrationTestData.item(itemRepository, owner, "Table", true);
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
