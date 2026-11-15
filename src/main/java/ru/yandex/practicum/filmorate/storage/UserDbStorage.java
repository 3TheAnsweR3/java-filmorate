package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public User createNewUser(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, new String[]{"id"}
            );
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setDate(4, Date.valueOf(user.getBirthday()));
            return statement;
        }, keyHolder);

        user.setId(keyHolder.getKey().longValue());
        return user;
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int updatedRows = jdbcTemplate.update(
                sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException(
                    "Пользователь с id " + user.getId() + " не найден"
            );
        }

        String deleteSql = "DELETE FROM friendships WHERE user_id = ?";
        jdbcTemplate.update(deleteSql, user.getId());

        String insertSql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)";

        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<Long, FriendshipStatus> entry : user.getFriends().entrySet()) {
            rows.add(new Object[]{user.getId(), entry.getKey(), entry.getValue().name()});
        }
        if (!rows.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, rows);
        }

        return user;
    }

    @Override
    public User getUser(long id) {
        String sql = "SELECT id, email, login, name, birthday FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
            user.setFriends(getFriends(id));
            return user;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Пользователь с id " + id + " не найден");
        }
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT id, email, login, name, birthday FROM users ORDER BY id";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);

        if (users.isEmpty()) {
            return users;
        }

        Map<Long, User> usersById = new HashMap<>();
        for (User user : users) {
            usersById.put(user.getId(), user);
        }

        String friendshipsSql = "SELECT user_id, friend_id, status FROM friendships WHERE user_id IN (:ids)";
        namedJdbcTemplate.query(friendshipsSql, Map.of("ids", usersById.keySet()), resultSet -> {
            User user = usersById.get(resultSet.getLong("user_id"));
            long friendId = resultSet.getLong("friend_id");
            FriendshipStatus status = FriendshipStatus.valueOf(resultSet.getString("status"));
            user.getFriends().put(friendId, status);
        });

        return users;
    }

    @Transactional
    @Override
    public void deleteUser(long id) {
        String deleteFriendshipsSql =
                "DELETE FROM friendships WHERE user_id = ? OR friend_id = ?";
        jdbcTemplate.update(deleteFriendshipsSql, id, id);
        jdbcTemplate.update("DELETE FROM likes WHERE user_id = ?", id);

        String sql = "DELETE FROM users WHERE id = ?";
        int deletedRows = jdbcTemplate.update(sql, id);

        if (deletedRows == 0) {
            throw new NotFoundException(
                    "Пользователь с id " + id + " не найден"
            );
        }
    }

    private User mapRowToUser(ResultSet resultSet, int rowNum) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setEmail(resultSet.getString("email"));
        user.setLogin(resultSet.getString("login"));
        user.setName(resultSet.getString("name"));
        user.setBirthday(resultSet.getDate("birthday").toLocalDate());
        return user;
    }

    private Map<Long, FriendshipStatus> getFriends(long userId) {
        String sql = "SELECT friend_id, status FROM friendships WHERE user_id = ?";

        return jdbcTemplate.query(sql, resultSet -> {
            Map<Long, FriendshipStatus> friends = new HashMap<>();

            while (resultSet.next()) {
                long friendId = resultSet.getLong("friend_id");
                FriendshipStatus status = FriendshipStatus.valueOf(
                        resultSet.getString("status")
                );
                friends.put(friendId, status);
            }

            return friends;
        }, userId);
    }
}
