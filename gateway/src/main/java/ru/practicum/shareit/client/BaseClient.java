package ru.practicum.shareit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

public abstract class BaseClient {
    protected static final String USER_HEADER = "X-Sharer-User-Id";

    private final RestTemplate restTemplate;
    private final String serverUrl;

    protected BaseClient(RestTemplate restTemplate, @Value("${shareit-server.url}") String serverUrl) {
        this.restTemplate = restTemplate;
        this.serverUrl = serverUrl;
    }

    protected Object get(String path) {
        return exchange(path, HttpMethod.GET, null, null);
    }

    protected Object get(String path, Long userId) {
        return exchange(path, HttpMethod.GET, null, userId);
    }

    protected Object post(String path, Object body, Long userId) {
        return exchange(path, HttpMethod.POST, body, userId);
    }

    protected Object post(String path, Object body) {
        return exchange(path, HttpMethod.POST, body, null);
    }

    protected Object patch(String path, Object body, Long userId) {
        return exchange(path, HttpMethod.PATCH, body, userId);
    }

    protected void delete(String path) {
        exchange(path, HttpMethod.DELETE, null, null);
    }

    private Object exchange(String path, HttpMethod method, Object body, Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (userId != null) {
            headers.set(USER_HEADER, userId.toString());
        }
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(serverUrl + path, method, request, Object.class).getBody();
    }

    protected String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
