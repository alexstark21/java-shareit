package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.*;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.CommentDto;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.ItemDto;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.ItemRequestDto;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.UserDto;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, ItemController.class,
        BookingController.class, ItemRequestController.class})
class ServerControllerMockMvcTest {
    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ItemService itemService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private ItemRequestService requestService;

    @Test
    void shouldHandleUserEndpoints() throws Exception {
        UserDto user = new UserDto(1L, "Alex", "alex@example.com");
        when(userService.createUser(any())).thenReturn(user);
        when(userService.updateUser(eq(1L), any())).thenReturn(user);
        when(userService.getUserById(1L)).thenReturn(user);
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex\",\"email\":\"alex@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(patch("/users/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"Alex\",\"email\":\"alex@example.com\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"));
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(delete("/users/1")).andExpect(status().isOk());

        verify(userService).createUser(any());
        verify(userService).updateUser(eq(1L), any());
        verify(userService).deleteUser(1L);
    }

    @Test
    void shouldHandleItemEndpoints() throws Exception {
        ItemDto item = new ItemDto();
        item.setId(2L);
        item.setName("Drill");
        item.setDescription("Cordless drill");
        item.setAvailable(true);
        CommentDto comment = new CommentDto();
        comment.setId(5L);
        comment.setText("Good");
        comment.setAuthorName("Alex");
        comment.setCreated(LocalDateTime.now());
        when(itemService.createItem(eq(1L), any())).thenReturn(item);
        when(itemService.updateItem(eq(1L), eq(2L), any())).thenReturn(item);
        when(itemService.getItemById(2L, 1L)).thenReturn(item);
        when(itemService.getOwnerItems(1L)).thenReturn(List.of(item));
        when(itemService.searchItems("drill")).thenReturn(List.of(item));
        when(itemService.addComment(eq(1L), eq(2L), any())).thenReturn(comment);

        mockMvc.perform(post("/items").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Cordless\",\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
        mockMvc.perform(patch("/items/2").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New drill\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/2").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
        mockMvc.perform(get("/items/search").param("text", "drill"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/items/2/comment").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value("Alex"));

        verify(itemService).createItem(eq(1L), any());
        verify(itemService).updateItem(eq(1L), eq(2L), any());
        verify(itemService).searchItems("drill");
        verify(itemService).addComment(eq(1L), eq(2L), any());
    }

    @Test
    void shouldHandleBookingEndpoints() throws Exception {
        BookingDto booking = new BookingDto();
        booking.setId(3L);
        booking.setStart(LocalDateTime.now().plusDays(1));
        booking.setEnd(LocalDateTime.now().plusDays(2));
        booking.setStatus(BookingStatus.WAITING);
        when(bookingService.create(eq(1L), any())).thenReturn(booking);
        when(bookingService.approve(1L, 3L, true)).thenReturn(booking);
        when(bookingService.get(1L, 3L)).thenReturn(booking);
        when(bookingService.getByBooker(1L, BookingState.WAITING)).thenReturn(List.of(booking));
        when(bookingService.getByOwner(1L, BookingState.ALL)).thenReturn(List.of(booking));

        mockMvc.perform(post("/bookings").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":2,\"start\":\"2026-11-06T10:00:00\","
                                + "\"end\":\"2026-11-07T10:00:00\"}"))
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

        verify(bookingService).create(eq(1L), any());
        verify(bookingService).approve(1L, 3L, true);
        verify(bookingService).getByBooker(1L, BookingState.WAITING);
        verify(bookingService).getByOwner(1L, BookingState.ALL);
    }

    @Test
    void shouldHandleItemRequestEndpoints() throws Exception {
        ItemRequestDto request = new ItemRequestDto();
        request.setId(4L);
        request.setDescription("Need a tent");
        request.setRequestorId(1L);
        request.setCreated(LocalDateTime.now());
        request.setItems(List.of());
        when(requestService.create(eq(1L), any())).thenReturn(request);
        when(requestService.getOwn(1L)).thenReturn(List.of(request));
        when(requestService.getAllOthers(1L)).thenReturn(List.of(request));
        when(requestService.getById(4L)).thenReturn(request);

        mockMvc.perform(post("/requests").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Need a tent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
        mockMvc.perform(get("/requests").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Need a tent"));
        mockMvc.perform(get("/requests/all").header(USER_HEADER, 1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/requests/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestorId").value(1));

        verify(requestService).create(eq(1L), any());
        verify(requestService).getOwn(1L);
        verify(requestService).getAllOthers(1L);
        verify(requestService).getById(4L);
    }

    @Test
    void shouldMapServiceNotFoundExceptionToNotFoundResponse() throws Exception {
        when(userService.getUserById(404L)).thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/users/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Пользователь не найден"));
    }

    @Test
    void shouldRejectEndpointsWithoutRequiredUserHeader() throws Exception {
        mockMvc.perform(get("/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/requests"))
                .andExpect(status().isBadRequest());
    }
}
