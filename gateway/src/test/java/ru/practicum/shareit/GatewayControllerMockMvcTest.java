package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.request.ItemRequestClient;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.UserController;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, ItemController.class,
        BookingController.class, ItemRequestController.class})
class GatewayControllerMockMvcTest {
    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserClient userClient;

    @MockBean
    private ItemClient itemClient;

    @MockBean
    private BookingClient bookingClient;

    @MockBean
    private ItemRequestClient requestClient;

    @Test
    void shouldHandleUserEndpoints() throws Exception {
        when(userClient.create(any())).thenReturn(Map.of("id", 1, "name", "Alex"));
        when(userClient.update(eq(1L), any())).thenReturn(Map.of("id", 1, "name", "Alex"));
        when(userClient.get(1L)).thenReturn(Map.of("id", 1, "email", "alex@example.com"));
        when(userClient.getAll()).thenReturn(java.util.List.of(Map.of("id", 1)));

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex\",\"email\":\"alex@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex Updated\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"));
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(delete("/users/1")).andExpect(status().isOk());

        verify(userClient).create(any());
        verify(userClient).update(eq(1L), any());
        verify(userClient).deleteUser(1L);
    }

    @Test
    void shouldRejectInvalidUserDataBeforeCallingServer() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(userClient);
    }

    @Test
    void shouldForwardServerHttpErrorsWithoutChangingStatus() throws Exception {
        when(userClient.get(404L)).thenThrow(new HttpClientErrorException(
                HttpStatus.NOT_FOUND, "Not Found", "{\"error\":\"User not found\"}".getBytes(),
                java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/users/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void shouldHandleItemEndpointsAndForwardRequestId() throws Exception {
        when(itemClient.create(eq(1L), any())).thenReturn(Map.of("id", 2, "requestId", 77));
        when(itemClient.update(eq(1L), eq(2L), any())).thenReturn(Map.of("id", 2));
        when(itemClient.get(1L, 2L)).thenReturn(Map.of("id", 2));
        when(itemClient.getByOwner(1L)).thenReturn(java.util.List.of(Map.of("id", 2)));
        when(itemClient.search("blue drill")).thenReturn(java.util.List.of(Map.of("id", 2)));
        when(itemClient.addComment(eq(1L), eq(2L), any())).thenReturn(Map.of("id", 5));

        mockMvc.perform(post("/items").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Cordless drill\","
                                + "\"available\":true,\"requestId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(77));
        mockMvc.perform(patch("/items/2").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/2").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/search").param("text", "blue drill"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/items/2/comment").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Works well\"}"))
                .andExpect(status().isOk());

        verify(itemClient).create(eq(1L), argThat(dto -> dto.toString().contains("requestId=77")));
        verify(itemClient).update(eq(1L), eq(2L), any());
        verify(itemClient).search("blue drill");
        verify(itemClient).addComment(eq(1L), eq(2L), any());
    }

    @Test
    void shouldValidateItemAndCommentRequestsBeforeCallingClient() throws Exception {
        mockMvc.perform(post("/items").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"description\":\"desc\",\"available\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/items/2/comment").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(itemClient);
    }

    @Test
    void shouldHandleBookingEndpoints() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusDays(1);
        when(bookingClient.create(eq(1L), any())).thenReturn(Map.of("id", 3, "status", "WAITING"));
        when(bookingClient.approve(1L, 3L, true)).thenReturn(Map.of("id", 3));
        when(bookingClient.get(1L, 3L)).thenReturn(Map.of("id", 3));
        when(bookingClient.getByBooker(1L, "WAITING")).thenReturn(java.util.List.of(Map.of("id", 3)));
        when(bookingClient.getByOwner(1L, "ALL")).thenReturn(java.util.List.of(Map.of("id", 3)));

        String body = "{\"itemId\":2,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";
        mockMvc.perform(post("/bookings").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
        mockMvc.perform(patch("/bookings/3").header(USER_HEADER, 1).param("approved", "true"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/3").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings").header(USER_HEADER, 1).param("state", "WAITING"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/owner").header(USER_HEADER, 1))
                .andExpect(status().isOk());

        verify(bookingClient).create(eq(1L), any());
        verify(bookingClient).approve(1L, 3L, true);
        verify(bookingClient).getByBooker(1L, "WAITING");
        verify(bookingClient).getByOwner(1L, "ALL");
    }

    @Test
    void shouldRejectInvalidBookingDatesAndMissingRequiredFields() throws Exception {
        String past = LocalDateTime.now().minusDays(2).toString();
        String future = LocalDateTime.now().plusDays(2).toString();
        mockMvc.perform(post("/bookings").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":2,\"start\":\"" + future
                                + "\",\"end\":\"" + past + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/bookings").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":2}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bookingClient);
    }

    @Test
    void shouldHandleItemRequestEndpoints() throws Exception {
        when(requestClient.create(eq(1L), any())).thenReturn(Map.of("id", 4));
        when(requestClient.getOwn(1L)).thenReturn(java.util.List.of(Map.of("id", 4)));
        when(requestClient.getAll(1L)).thenReturn(java.util.List.of(Map.of("id", 5)));
        when(requestClient.getById(4L)).thenReturn(Map.of("id", 4));

        mockMvc.perform(post("/requests").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Need a tent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
        mockMvc.perform(get("/requests").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/requests/all").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/requests/4"))
                .andExpect(status().isOk());

        verify(requestClient).create(eq(1L), any());
        verify(requestClient).getOwn(1L);
        verify(requestClient).getAll(1L);
        verify(requestClient).getById(4L);
    }

    @Test
    void shouldRejectInvalidRequestDescription() throws Exception {
        mockMvc.perform(post("/requests").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestClient);
    }

    @Test
    void shouldRejectRequestsWithoutRequiredHeaders() throws Exception {
        mockMvc.perform(get("/items")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/bookings")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/requests")).andExpect(status().isBadRequest());
    }
}
