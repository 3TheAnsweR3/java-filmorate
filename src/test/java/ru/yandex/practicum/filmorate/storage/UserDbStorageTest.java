package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
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
        User user = buildUser(
                "ivan@example.com",
                "ivan",
                "Иван",
                LocalDate.of(1990, 5, 15)
        );

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
        User firstUser = buildUser(
                "ivan@example.com",
                "ivan",
                "Иван",
                LocalDate.of(1990, 5, 15)
        );

        User secondUser = buildUser(
                "alex@example.com",
                "alex",
                "Алексей",
                LocalDate.of(1995, 8, 20)
        );

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
        User user = buildUser(
                "ivan@example.com",
                "ivan",
                "Иван",
                LocalDate.of(1990, 5, 15)
        );

        userStorage.createNewUser(user);

        User updatedUser = buildUser(
                "new@example.com",
                "ivan_new",
                "Новое имя",
                LocalDate.of(1991, 6, 16)
        );

        updatedUser.setId(user.getId());

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
        User user = buildUser(

                "unknown@example.com",
                "unknown",
                "Неизвестный",
                LocalDate.of(1990, 5, 15)
        );
        user.setId(999L);

        assertThrows(
                NotFoundException.class,
                () -> userStorage.updateUser(user)
        );
    }

    @Test
    @DisplayName("Удалённый пользователь больше не доступен по ID")
    void shouldDeleteUser() {
        User user = buildUser(
                "ivan@example.com",
                "ivan",
                "Иван",
                LocalDate.of(1990, 5, 15)
        );

        userStorage.createNewUser(user);

        userStorage.deleteUser(user.getId());

        assertThrows(
                NotFoundException.class,
                () -> userStorage.getUser(user.getId())
        );
        assertTrue(userStorage.getAllUsers().isEmpty());
    }

    @Test
    @DisplayName("Удаление несуществующего пользователя вызывает NotFoundException")
    void shouldThrowWhenDeletingUnknownUser() {
        assertThrows(
                NotFoundException.class,
                () -> userStorage.deleteUser(999L)
        );
    }

    @Test
    @DisplayName("Обновление пользователя сохраняет исходящую связь с другом и её статус")
    void shouldSaveFriendshipWhenUpdatingUser() {
        User user = buildUser(
                "ivan@example.com", "ivan", "Иван",
                LocalDate.of(1990, 5, 15)
        );
        User friend = buildUser(
                "alex@example.com", "alex", "Алексей",
                LocalDate.of(1995, 8, 20)
        );

        userStorage.createNewUser(user);
        userStorage.createNewUser(friend);

        user.getFriends().put(friend.getId(), FriendshipStatus.UNCONFIRMED);
        userStorage.updateUser(user);

        User foundUser = userStorage.getUser(user.getId());
        User foundFriend = userStorage.getUser(friend.getId());

        assertEquals(1, foundUser.getFriends().size());
        assertEquals(
                FriendshipStatus.UNCONFIRMED,
                foundUser.getFriends().get(friend.getId())
        );
        assertTrue(foundFriend.getFriends().isEmpty());
    }

    @Test
    @DisplayName("Список пользователей содержит сохранённые связи с друзьями")
    void shouldGetAllUsersWithFriendships() {
        User user = buildUser(
                "ivan@example.com", "ivan", "Иван",
                LocalDate.of(1990, 5, 15)
        );
        User friend = buildUser(
                "alex@example.com", "alex", "Алексей",
                LocalDate.of(1995, 8, 20)
        );

        userStorage.createNewUser(user);
        userStorage.createNewUser(friend);

        user.getFriends().put(friend.getId(), FriendshipStatus.UNCONFIRMED);
        userStorage.updateUser(user);

        List<User> users = userStorage.getAllUsers();

        User foundUser = users.stream()
                .filter(item -> item.getId() == user.getId())
                .findFirst()
                .orElseThrow();

        assertEquals(1, foundUser.getFriends().size());
        assertEquals(
                FriendshipStatus.UNCONFIRMED,
                foundUser.getFriends().get(friend.getId())
        );
    }

    @Test
    @DisplayName("При удалении пользователя удаляются его входящие и исходящие связи")
    void shouldDeleteUserWithFriendships() {
        User user = buildUser(
                "ivan@example.com", "ivan", "Иван",
                LocalDate.of(1990, 5, 15)
        );
        User friend = buildUser(
                "alex@example.com", "alex", "Алексей",
                LocalDate.of(1995, 8, 20)
        );

        userStorage.createNewUser(user);
        userStorage.createNewUser(friend);

        user.getFriends().put(friend.getId(), FriendshipStatus.CONFIRMED);
        friend.getFriends().put(user.getId(), FriendshipStatus.CONFIRMED);
        userStorage.updateUser(user);
        userStorage.updateUser(friend);

        userStorage.deleteUser(user.getId());

        assertThrows(
                NotFoundException.class,
                () -> userStorage.getUser(user.getId())
        );

        User remainingFriend = userStorage.getUser(friend.getId());
        assertTrue(remainingFriend.getFriends().isEmpty());
    }

    @Test
    @DisplayName("При отсутствии пользователей возвращается пустой список")
    void shouldReturnEmptyListWhenNoUsers() {
        List<User> users = userStorage.getAllUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    private User buildUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        return user;
    }
}
