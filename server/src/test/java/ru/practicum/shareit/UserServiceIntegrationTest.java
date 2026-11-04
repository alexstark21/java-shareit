package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserDto;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateAndReadUser() {
        UserDto dto = new UserDto(null, "Alex", "alex@example.com");

        UserDto created = userService.createUser(dto);
        UserDto found = userService.getUserById(created.getId());

        assertNotNull(created.getId());
        assertEquals("Alex", found.getName());
        assertEquals("alex@example.com", found.getEmail());
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        IntegrationTestData.user(userRepository, "First", "same@example.com");
        UserDto duplicate = new UserDto(null, "Second", "SAME@example.com");

        assertThrows(ConflictException.class, () -> userService.createUser(duplicate));
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        User user = IntegrationTestData.user(userRepository, "Old name", "old@example.com");
        UserDto update = new UserDto(null, "New name", null);

        UserDto updated = userService.updateUser(user.getId(), update);

        assertEquals("New name", updated.getName());
        assertEquals("old@example.com", updated.getEmail());
    }

    @Test
    void shouldRejectEmailAlreadyUsedByAnotherUser() {
        User first = IntegrationTestData.user(userRepository, "First", "first@example.com");
        IntegrationTestData.user(userRepository, "Second", "second@example.com");
        UserDto update = new UserDto(null, "First", "second@example.com");

        assertThrows(ConflictException.class, () -> userService.updateUser(first.getId(), update));
    }

    @Test
    void shouldReturnAllUsers() {
        IntegrationTestData.user(userRepository, "First", "first@example.com");
        IntegrationTestData.user(userRepository, "Second", "second@example.com");

        Collection<UserDto> users = userService.getAllUsers();

        assertEquals(2, users.size());
    }

    @Test
    void shouldDeleteExistingUser() {
        User user = IntegrationTestData.user(userRepository, "Delete", "delete@example.com");

        userService.deleteUser(user.getId());

        assertFalse(userRepository.existsById(user.getId()));
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        assertThrows(NotFoundException.class, () -> userService.getUserById(-1L));
    }
}
