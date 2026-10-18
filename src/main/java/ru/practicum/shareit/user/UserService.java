package ru.practicum.shareit.user;

import java.util.Collection;

public interface UserService {
    UserDto createUser(UserDto userDto);

    UserDto updateUser(Long userId, UserDto userDto);

    UserDto getUserById(Long userId);

    Collection<UserDto> getAllUsers();

    void deleteUser(Long userId);
}
