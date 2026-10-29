package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(UserDbStorage.class)
@JdbcTest
@DisplayName("Хранение пользователей в базе данных")
public class UserDbStorageTest {
    @Autowired
    private UserDbStorage userStorage;

    @Test
    @DisplayName("Созданный пользователь доступен по ID со всеми сохранёнными полями")
    void shouldCreateAndGetUserById() {
        User user = new User();
        user.setEmail("ivan@example.com");
        user.setLogin("ivan");
        user.setName("Иван");
        user.setBirthday(LocalDate.of(1990, 5, 15));

        User createdUser = userStorage.createNewUser(user);
        assertNotNull(createdUser);

        User foundUser = userStorage.getUser(createdUser.getId());

        assertNotNull(foundUser);
        assertTrue(createdUser.getId() > 0);
        assertEquals(createdUser.getId(), foundUser.getId());
        assertEquals("ivan@example.com", foundUser.getEmail());
        assertEquals("ivan", foundUser.getLogin());
        assertEquals("Иван", foundUser.getName());
        assertEquals(LocalDate.of(1990, 5, 15), foundUser.getBirthday());
    }

    @Test
    @DisplayName("Получение несуществующего пользователя вызывает NotFoundException")
    void shouldThrowWhenUserNotFound() {
        assertThrows(NotFoundException.class, () -> userStorage.getUser(999L));
    }

    @Test
    @DisplayName("Список пользователей содержит всех сохранённых пользователей")
    void shouldGetAllUsers() {
        User firstUser = new User();
        firstUser.setEmail("ivan@example.com");
        firstUser.setLogin("ivan");
        firstUser.setName("Иван");
        firstUser.setBirthday(LocalDate.of(1990, 5, 15));

        User secondUser = new User();
        secondUser.setEmail("alex@example.com");
        secondUser.setLogin("alex");
        secondUser.setName("Алексей");
        secondUser.setBirthday(LocalDate.of(1995, 8, 20));

        userStorage.createNewUser(firstUser);
        userStorage.createNewUser(secondUser);

        List<User> users = userStorage.getAllUsers();

        assertEquals(2, users.size());
        assertTrue(users.stream()
                .anyMatch(user -> user.getId() == firstUser.getId()));
        assertTrue(users.stream()
                .anyMatch(user -> user.getId() == secondUser.getId()));
    }

    @Test
    @DisplayName("Обновление пользователя сохраняет новые данные без создания новой записи")
    void shouldUpdateUser() {
        User user = new User();
        user.setEmail("ivan@example.com");
        user.setLogin("ivan");
        user.setName("Иван");
        user.setBirthday(LocalDate.of(1990, 5, 15));
        userStorage.createNewUser(user);

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setEmail("new@example.com");
        updatedUser.setLogin("ivan_new");
        updatedUser.setName("Новое имя");
        updatedUser.setBirthday(LocalDate.of(1991, 6, 16));

        User result = userStorage.updateUser(updatedUser);

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());

        User foundUser = userStorage.getUser(user.getId());

        assertEquals(user.getId(), foundUser.getId());
        assertEquals("new@example.com", foundUser.getEmail());
        assertEquals("ivan_new", foundUser.getLogin());
        assertEquals("Новое имя", foundUser.getName());
        assertEquals(LocalDate.of(1991, 6, 16), foundUser.getBirthday());
        assertEquals(1, userStorage.getAllUsers().size());
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя вызывает NotFoundException")
    void shouldThrowWhenUpdatingUnknownUser() {
        User user = new User();
        user.setId(999L);
        user.setEmail("unknown@example.com");
        user.setLogin("unknown");
        user.setName("Неизвестный");
        user.setBirthday(LocalDate.of(1990, 5, 15));

        assertThrows(
                NotFoundException.class,
                () -> userStorage.updateUser(user)
        );
    }

    @Test
    @DisplayName("При отсутствии пользователей возвращается пустой список")
    void shouldReturnEmptyListWhenNoUsers() {
        List<User> users = userStorage.getAllUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }
}
