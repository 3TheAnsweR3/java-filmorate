package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserStorage {

    public User createNewUser(User user);

    public User updateUser(User user);

    public User getUser(long id);

    public List<User> getAllUsers();

    public void deleteUser(long id);
}
