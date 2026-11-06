package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;

@Component
public class ItemClient extends BaseClient {
    public ItemClient(RestTemplate restTemplate, @Value("${shareit-server.url}") String serverUrl) {
        super(restTemplate, serverUrl);
    }

    public Object create(Long userId, Object dto) {
        return post("/items", dto, userId);
    }

    public Object update(Long userId, Long itemId, Object dto) {
        return patch("/items/" + itemId, dto, userId);
    }

    public Object get(Long userId, Long itemId) {
        return get("/items/" + itemId, userId);
    }

    public Object getByOwner(Long userId) {
        return get("/items", userId);
    }

    public Object search(String text) {
        return get("/items/search?text=" + encode(text));
    }

    public Object addComment(Long userId, Long itemId, Object dto) {
        return post("/items/" + itemId + "/comment", dto, userId);
    }
}
