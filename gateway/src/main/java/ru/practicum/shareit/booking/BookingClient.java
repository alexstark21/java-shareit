package ru.practicum.shareit.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;

@Component
public class BookingClient extends BaseClient {
    public BookingClient(RestTemplate restTemplate, @Value("${shareit-server.url}") String serverUrl) {
        super(restTemplate, serverUrl);
    }

    public Object create(Long userId, Object dto) {
        return post("/bookings", dto, userId);
    }

    public Object approve(Long userId, Long bookingId, boolean approved) {
        return patch("/bookings/" + bookingId + "?approved=" + approved, null, userId);
    }

    public Object get(Long userId, Long bookingId) {
        return get("/bookings/" + bookingId, userId);
    }

    public Object getByBooker(Long userId, String state) {
        return get("/bookings?state=" + encode(state), userId);
    }

    public Object getByOwner(Long userId, String state) {
        return get("/bookings/owner?state=" + encode(state), userId);
    }
}
