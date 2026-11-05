package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film createNewFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration) "
                + "VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, new String[]{"id"}
            );
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            return statement;
        }, keyHolder);

        Number generatedId = Objects.requireNonNull(
                keyHolder.getKey(),
                "База данных не вернула ID созданного фильма"
        );
        film.setId(generatedId.longValue());
        return film;
    }

    @Override
    public Film updateFilm(Film updatedFilm) {
        String sql = "UPDATE films SET name = ?, description = ?, "
                + "release_date = ?, duration = ? WHERE id = ?";

        int updatedRows = jdbcTemplate.update(
                sql,
                updatedFilm.getName(),
                updatedFilm.getDescription(),
                Date.valueOf(updatedFilm.getReleaseDate()),
                updatedFilm.getDuration(),
                updatedFilm.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException(
                    "Фильм с id " + updatedFilm.getId() + " не найден"
            );
        }

        return updatedFilm;
    }

    @Override
    public Film getFilm(long id) {
        String sql = "SELECT id, name, description, release_date, duration "
                + "FROM films WHERE id = ?";

        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT id, name, description, release_date, duration "
                + "FROM films ORDER BY id";

        return jdbcTemplate.query(sql, this::mapRowToFilm);
    }

    @Override
    public void deleteFilm(long id) {
        String sql = "DELETE FROM films WHERE id = ?";
        int deletedRows = jdbcTemplate.update(sql, id);

        if (deletedRows == 0) {
            throw new NotFoundException(
                    "Фильм с id " + id + " не найден"
            );
        }
    }

    private Film mapRowToFilm(ResultSet resultSet, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(resultSet.getLong("id"));
        film.setName(resultSet.getString("name"));
        film.setDescription(resultSet.getString("description"));
        film.setReleaseDate(resultSet.getDate("release_date").toLocalDate());
        film.setDuration(resultSet.getInt("duration"));
        return film;
    }
}