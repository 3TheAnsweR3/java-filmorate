package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

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
        String sql = "INSERT INTO films (name, description, release_date, duration, rating_id) "
                + "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, new String[]{"id"}
            );
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            if (film.getMpa() == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, film.getMpa().getId());
            }
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
                + "release_date = ?, duration = ?, rating_id = ? WHERE id = ?";

        int updatedRows = jdbcTemplate.update(
                sql,
                updatedFilm.getName(),
                updatedFilm.getDescription(),
                Date.valueOf(updatedFilm.getReleaseDate()),
                updatedFilm.getDuration(),
                updatedFilm.getMpa() == null ? null : updatedFilm.getMpa().getId(),
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
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, "
                + "r.id AS mpa_id, r.name AS mpa_name "
                + "FROM films AS f "
                + "LEFT JOIN ratings AS r ON f.rating_id = r.id "
                + "WHERE f.id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, "
                + "r.id AS mpa_id, r.name AS mpa_name "
                + "FROM films AS f "
                + "LEFT JOIN ratings AS r ON f.rating_id = r.id "
                + "ORDER BY f.id";

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
        Integer mpaId = resultSet.getObject("mpa_id", Integer.class);
        if (mpaId != null) {
            Mpa mpa = new Mpa();
            mpa.setId(mpaId);
            mpa.setName(resultSet.getString("mpa_name"));
            film.setMpa(mpa);
        }
        return film;
    }
}