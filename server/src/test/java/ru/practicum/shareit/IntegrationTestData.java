package ru.practicum.shareit;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.CommentRepository;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;

public final class IntegrationTestData {
    private IntegrationTestData() {
    }

    public static User user(UserRepository repository, String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return repository.save(user);
    }

    public static Item item(ItemRepository repository, User owner, String name, boolean available) {
        return item(repository, owner, name, available, null);
    }

    public static Item item(ItemRepository repository, User owner, String name,
                            boolean available, Long requestId) {
        Item item = new Item();
        item.setName(name);
        item.setDescription("Description for " + name);
        item.setAvailable(available);
        item.setOwner(owner);
        item.setRequestId(requestId);
        return repository.save(item);
    }

    public static Booking booking(BookingRepository repository, Item item, User booker,
                                  LocalDateTime start, LocalDateTime end, BookingStatus status) {
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setStatus(status);
        return repository.save(booking);
    }

    public static Comment comment(CommentRepository repository, Item item, User author, String text) {
        Comment comment = new Comment();
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setText(text);
        comment.setCreated(LocalDateTime.now().minusMinutes(1));
        return repository.save(comment);
    }

    public static ItemRequest request(ItemRequestRepository repository, User requestor,
                                      String description, LocalDateTime created) {
        ItemRequest request = new ItemRequest();
        request.setRequestor(requestor);
        request.setDescription(description);
        request.setCreated(created);
        return repository.save(request);
    }
}
