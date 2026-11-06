package ru.practicum.shareit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.*;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceIntegrationTest {
    @Autowired
    private BookingService bookingService;

    @PersistenceContext
    private EntityManager entityManager;

    private User owner;
    private User booker;
    private User outsider;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = IntegrationTestData.user(entityManager, "Owner", "booking-owner@example.com");
        booker = IntegrationTestData.user(entityManager, "Booker", "booking-booker@example.com");
        outsider = IntegrationTestData.user(entityManager, "Outsider", "booking-outsider@example.com");
        item = IntegrationTestData.item(entityManager, owner, "Projector", true);
    }

    @Test
    void shouldCreateWaitingBooking() {
        BookingDto created = bookingService.create(booker.getId(), bookingDto());

        assertNotNull(created.getId());
        assertEquals(BookingStatus.WAITING, created.getStatus());
        assertEquals(item.getId(), created.getItem().getId());
        assertEquals(booker.getId(), created.getBooker().getId());
    }

    @Test
    void shouldRejectBookingOfOwnItem() {
        assertThrows(ConflictException.class,
                () -> bookingService.create(owner.getId(), bookingDto()));
    }

    @Test
    void shouldRejectBookingOfUnavailableItem() {
        item.setAvailable(false);
        entityManager.flush();

        assertThrows(BadRequestException.class,
                () -> bookingService.create(booker.getId(), bookingDto()));
    }

    @Test
    void shouldRejectInvalidBookingPeriod() {
        BookingCreateDto dto = bookingDto();
        dto.setStart(LocalDateTime.now().plusDays(2));
        dto.setEnd(LocalDateTime.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> bookingService.create(booker.getId(), dto));
    }

    @Test
    void shouldRejectOverlappingApprovedBooking() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(2);
        IntegrationTestData.booking(entityManager, item, outsider,
                start.minusMinutes(30), end.plusMinutes(30), BookingStatus.APPROVED);
        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(start);
        dto.setEnd(end);

        assertThrows(ConflictException.class, () -> bookingService.create(booker.getId(), dto));
    }

    @Test
    void shouldApproveBookingByOwner() {
        Booking booking = saveBooking(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                BookingStatus.WAITING);

        BookingDto approved = bookingService.approve(owner.getId(), booking.getId(), true);

        assertEquals(BookingStatus.APPROVED, approved.getStatus());
    }

    @Test
    void shouldRejectApprovalByNonOwner() {
        Booking booking = saveBooking(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                BookingStatus.WAITING);

        assertThrows(BadRequestException.class,
                () -> bookingService.approve(booker.getId(), booking.getId(), true));
    }

    @Test
    void shouldNotChangeAlreadyProcessedBooking() {
        Booking booking = saveBooking(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                BookingStatus.APPROVED);

        assertThrows(ConflictException.class,
                () -> bookingService.approve(owner.getId(), booking.getId(), false));
    }

    @Test
    void shouldReturnBookingToBookerAndOwnerOnly() {
        Booking booking = saveBooking(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                BookingStatus.WAITING);

        assertNotNull(bookingService.get(booker.getId(), booking.getId()));
        assertNotNull(bookingService.get(owner.getId(), booking.getId()));
        assertThrows(NotFoundException.class,
                () -> bookingService.get(outsider.getId(), booking.getId()));
    }

    @Test
    void shouldFilterBookerBookingsByAllTimeStates() {
        LocalDateTime now = LocalDateTime.now();
        saveBooking(now.minusHours(2), now.minusHours(1), BookingStatus.APPROVED);
        saveBooking(now.minusHours(1), now.plusHours(1), BookingStatus.APPROVED);
        saveBooking(now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        saveBooking(now.plusHours(3), now.plusHours(4), BookingStatus.REJECTED);

        assertEquals(4, bookingService.getByBooker(booker.getId(), BookingState.ALL).size());
        assertEquals(1, bookingService.getByBooker(booker.getId(), BookingState.PAST).size());
        assertEquals(1, bookingService.getByBooker(booker.getId(), BookingState.CURRENT).size());
        assertEquals(2, bookingService.getByBooker(booker.getId(), BookingState.FUTURE).size());
        assertEquals(1, bookingService.getByBooker(booker.getId(), BookingState.WAITING).size());
        assertEquals(1, bookingService.getByBooker(booker.getId(), BookingState.REJECTED).size());
    }

    @Test
    void shouldReturnOwnerBookings() {
        saveBooking(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), BookingStatus.WAITING);

        Collection<BookingDto> bookings = bookingService.getByOwner(owner.getId(), BookingState.ALL);

        assertEquals(1, bookings.size());
        assertEquals(booker.getId(), bookings.iterator().next().getBooker().getId());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> bookingService.getByBooker(-1L, BookingState.ALL));
    }

    private BookingCreateDto bookingDto() {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        return dto;
    }

    private Booking saveBooking(LocalDateTime start, LocalDateTime end, BookingStatus status) {
        return IntegrationTestData.booking(entityManager, item, booker, start, end, status);
    }
}
