package ru.practicum.shareit;

import jakarta.persistence.EntityManager;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;

public final class IntegrationTestData {
    private IntegrationTestData() {
    }

    public static User user(EntityManager entityManager, String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        entityManager.persist(user);
        return user;
    }

    public static Item item(EntityManager entityManager, User owner, String name, boolean available) {
        return item(entityManager, owner, name, available, null);
    }

    public static Item item(EntityManager entityManager, User owner, String name,
                            boolean available, Long requestId) {
        Item item = new Item();
        item.setName(name);
        item.setDescription("Description for " + name);
        item.setAvailable(available);
        item.setOwner(owner);
        item.setRequestId(requestId);
        entityManager.persist(item);
        return item;
    }

    public static Booking booking(EntityManager entityManager, Item item, User booker,
                                  LocalDateTime start, LocalDateTime end, BookingStatus status) {
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setStatus(status);
        entityManager.persist(booking);
        return booking;
    }

    public static Comment comment(EntityManager entityManager, Item item, User author, String text) {
        Comment comment = new Comment();
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setText(text);
        comment.setCreated(LocalDateTime.now().minusMinutes(1));
        entityManager.persist(comment);
        return comment;
    }

    public static ItemRequest request(EntityManager entityManager, User requestor,
                                      String description, LocalDateTime created) {
        ItemRequest request = new ItemRequest();
        request.setRequestor(requestor);
        request.setDescription(description);
        request.setCreated(created);
        entityManager.persist(request);
        return request;
    }
}
