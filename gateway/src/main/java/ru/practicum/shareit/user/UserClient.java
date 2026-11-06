package ru.practicum.shareit.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;

@Component
public class UserClient extends BaseClient {
    public UserClient(RestTemplate restTemplate, @Value("${shareit-server.url}") String serverUrl) {
        super(restTemplate, serverUrl);
    }

    public Object create(Object dto) {
        return post("/users", dto);
    }

    public Object update(Long userId, Object dto) {
        return patch("/users/" + userId, dto, null);
    }

    public Object get(Long userId) {
        return get("/users/" + userId);
    }

    public Object getAll() {
        return get("/users");
    }

    public void deleteUser(Long userId) {
        delete("/users/" + userId);
    }
}
