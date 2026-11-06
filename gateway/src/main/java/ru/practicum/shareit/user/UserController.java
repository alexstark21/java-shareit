package ru.practicum.shareit.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserClient client;

    @PostMapping
    public Object create(@Valid @RequestBody UserDto dto) {
        return client.create(dto);
    }

    @PatchMapping("/{userId}")
    public Object update(@PathVariable Long userId, @Valid @RequestBody UserUpdateDto dto) {
        return client.update(userId, dto);
    }

    @GetMapping("/{userId}")
    public Object get(@PathVariable Long userId) {
        return client.get(userId);
    }

    @GetMapping
    public Object getAll() {
        return client.getAll();
    }

    @DeleteMapping("/{userId}")
    public void delete(@PathVariable Long userId) {
        client.deleteUser(userId);
    }
}
