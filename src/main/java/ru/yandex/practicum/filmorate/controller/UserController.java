package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final List<User> users = new ArrayList<>();
    private int id = 0;

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        validateUser(user);

        user.setId(++id);
        users.add(user);

        log.info("Создан пользователь с id={}", user.getId());

        return user;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return users;
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User updatedUser) {
        validateUser(updatedUser);

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == updatedUser.getId()) {
                users.set(i, updatedUser);

                log.info("Обновлён пользователь с id={}", updatedUser.getId());

                return updatedUser;
            }
        }
        log.warn("Не удалось обновить пользователя: id={} не найден",
                updatedUser.getId());
        throw new ValidationException(
                "Пользователь с id " + updatedUser.getId() + " не найден!");
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Ошибка валидации пользователя: некорректный email");
            throw new ValidationException("Email пользователя не должен быть пустым и должен содержать @!");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Ошибка валидации пользователя: некорректный логин");
            throw new ValidationException("Логин пользователя не должен быть пустым и содержать пробелов!");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Пользователь оставил имя пустым");
            user.setName(user.getLogin());
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Ошибка валидации пользователя: некорректная дата рождения {}",
                    user.getBirthday());
            throw new ValidationException("День рождения пользователя не должен быть позднее настоящей даты!");
        }
    }
}
