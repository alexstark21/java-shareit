package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserDto createUser(UserDto userDto) {
        validateEmailUniqueness(userDto.getEmail());

        User user = UserMapper.toUser(userDto);
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        User user = findUserOrThrow(userId);

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()
                && !userDto.getEmail().equalsIgnoreCase(user.getEmail())) {

            validateEmailUniqueness(userDto.getEmail());
            user.setEmail(userDto.getEmail());
        }

        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            user.setName(userDto.getName());
        }

        return UserMapper.toUserDto(userRepository.update(user));
    }

    @Override
    public UserDto getUserById(Long userId) {
        return UserMapper.toUserDto(findUserOrThrow(userId));
    }

    @Override
    public Collection<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long userId) {
        findUserOrThrow(userId);
        userRepository.delete(userId);
    }

    private void validateEmailUniqueness(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        boolean exists = userRepository.findAll().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));

        if (exists) {
            throw new ConflictException("Email " + email + " уже занят");
        }
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }
}
