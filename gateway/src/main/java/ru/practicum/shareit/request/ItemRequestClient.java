package ru.practicum.shareit.request;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;

@Component
public class ItemRequestClient extends BaseClient {
    public ItemRequestClient(RestTemplate restTemplate, @Value("${shareit-server.url}") String serverUrl) {
        super(restTemplate, serverUrl);
    }

    public Object create(Long userId, Object dto) {
        return post("/requests", dto, userId);
    }

    public Object getOwn(Long userId) {
        return get("/requests", userId);
    }

    public Object getAll(Long userId) {
        return get("/requests/all", userId);
    }

    public Object getById(Long requestId) {
        return get("/requests/" + requestId);
    }
}
