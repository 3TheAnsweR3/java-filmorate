package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

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
}
