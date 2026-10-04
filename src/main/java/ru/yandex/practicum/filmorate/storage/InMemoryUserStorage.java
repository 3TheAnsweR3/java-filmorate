package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long id = 0;

    @Override
    public User createNewUser(User user) {
        user.setId(++id);
        users.put(user.getId(), user);
        log.info("Сохранён пользователь с id={}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User updatedUser) {
        checkUserExists(updatedUser.getId());
        users.put(updatedUser.getId(), updatedUser);
        log.info("Обновлён пользователь с id={}", updatedUser.getId());
        return updatedUser;
    }

    @Override
    public User getUser(long id) {
        checkUserExists(id);
        return users.get(id);
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void deleteUser(long id) {
        checkUserExists(id);
        users.remove(id);
        log.info("Удалён пользователь с id={}", id);
    }

    private void checkUserExists(long id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id " + id + " не найден");
        }
    }
}
