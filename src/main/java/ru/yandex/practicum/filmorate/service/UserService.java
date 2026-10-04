package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User createUser(User user) {
        prepareUser(user);
        user.setFriends(new HashSet<>());
        return userStorage.createNewUser(user);
    }

    public User updateUser(User updatedUser) {
        prepareUser(updatedUser);
        User storedUser = userStorage.getUser(updatedUser.getId());
        updatedUser.setFriends(storedUser.getFriends());
        return userStorage.updateUser(updatedUser);
    }

    public User getUser(long id) {
        return userStorage.getUser(id);
    }

    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public void deleteUser(long id) {
        userStorage.deleteUser(id);
    }

    public void addFriend(long userId, long friendId) {
        if (userId == friendId) {
            throw new ValidationException(
                    "Пользователь не может добавить себя в друзья");
        }

        User user = userStorage.getUser(userId);
        User friend = userStorage.getUser(friendId);

        boolean userChanged = user.getFriends().add(friendId);
        boolean friendChanged = friend.getFriends().add(userId);

        if (userChanged) {
            userStorage.updateUser(user);
        }
        if (friendChanged) {
            userStorage.updateUser(friend);
        }

        log.info("Пользователи с id={} и id={} стали друзьями",
                userId, friendId);
    }

    public void removeFriend(long userId, long friendId) {
        User user = userStorage.getUser(userId);
        User friend = userStorage.getUser(friendId);

        boolean userChanged = user.getFriends().remove(friendId);
        boolean friendChanged = friend.getFriends().remove(userId);

        if (userChanged) {
            userStorage.updateUser(user);
        }
        if (friendChanged) {
            userStorage.updateUser(friend);
        }

        log.info("Пользователи с id={} и id={} больше не друзья",
                userId, friendId);
    }

    public List<User> getFriends(long userId) {
        User user = userStorage.getUser(userId);

        return user.getFriends()
                .stream()
                .map(userStorage::getUser)
                .toList();
    }

    public List<User> getCommonFriends(long userId, long otherId) {
        User user = userStorage.getUser(userId);
        User other = userStorage.getUser(otherId);

        Set<Long> commonFriendIds = new HashSet<>(user.getFriends());
        commonFriendIds.retainAll(other.getFriends());

        return commonFriendIds
                .stream()
                .map(userStorage::getUser)
                .toList();
    }

    private void prepareUser(User user) {
        if (user.getLogin() != null && user.getLogin().contains(" ")) {
            throw new ValidationException(
                    "Логин пользователя не должен содержать пробелов");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
