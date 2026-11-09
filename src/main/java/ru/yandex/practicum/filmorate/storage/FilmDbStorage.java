package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
public class FilmDbStorage implements FilmStorage {
    private static final String SELECT_FILMS =
            "SELECT f.id, f.name, f.description, f.release_date, f.duration, "
                    + "r.id AS mpa_id, r.name AS mpa_name "
                    + "FROM films AS f "
                    + "LEFT JOIN ratings AS r ON f.rating_id = r.id";
    private static final String SELECT_FILM_GENRES =
            "SELECT fg.film_id, g.id, g.name FROM film_genres AS fg "
                    + "JOIN genres AS g ON fg.genre_id = g.id";
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private final MpaDbStorage mpaDbStorage;

    public FilmDbStorage(JdbcTemplate jdbcTemplate, MpaDbStorage mpaDbStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.mpaDbStorage = mpaDbStorage;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    @Transactional
    public Film createNewFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, rating_id) "
                + "VALUES (?, ?, ?, ?, ?)";

        Mpa mpa = resolveMpa(film.getMpa());
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, new String[]{"id"}
            );
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            if (mpa == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, mpa.getId());
            }
            return statement;
        }, keyHolder);

        Number generatedId = Objects.requireNonNull(
                keyHolder.getKey(),
                "База данных не вернула ID созданного фильма"
        );
        long filmId = generatedId.longValue();
        saveGenres(filmId, film.getGenres());
        film.setGenres(loadGenres(filmId));
        film.setLikes(new HashSet<>());
        film.setId(filmId);
        film.setMpa(mpa);
        return film;
    }

    @Override
    @Transactional
    public Film updateFilm(Film updatedFilm) {
        String sql = "UPDATE films SET name = ?, description = ?, "
                + "release_date = ?, duration = ?, rating_id = ? WHERE id = ?";

        Mpa mpa = resolveMpa(updatedFilm.getMpa());
        int updatedRows = jdbcTemplate.update(
                sql,
                updatedFilm.getName(),
                updatedFilm.getDescription(),
                Date.valueOf(updatedFilm.getReleaseDate()),
                updatedFilm.getDuration(),
                mpa == null ? null : mpa.getId(),
                updatedFilm.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException(
                    "Фильм с id " + updatedFilm.getId() + " не найден"
            );
        }

        deleteGenres(updatedFilm.getId());
        saveGenres(updatedFilm.getId(), updatedFilm.getGenres());
        updatedFilm.setGenres(loadGenres(updatedFilm.getId()));
        updatedFilm.setLikes(loadLikes(updatedFilm.getId()));
        updatedFilm.setMpa(mpa);
        return updatedFilm;
    }

    @Override
    public Film getFilm(long id) {
        String sql = SELECT_FILMS + " WHERE f.id = ?";
        Film film;
        try {
            film = jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
        film.setGenres(loadGenres(id));
        film.setLikes(loadLikes(id));
        return film;
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = SELECT_FILMS + " ORDER BY f.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        loadFilmRelations(films);
        return films;
    }

    @Override
    public void addLike(Film film, User user) {
        String sql = "MERGE INTO likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, film.getId(), user.getId());
    }

    @Override
    public void removeLike(Film film, User user) {
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ? AND user_id = ?",
                film.getId(), user.getId());
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = SELECT_FILMS
                + " LEFT JOIN (SELECT film_id, COUNT(*) AS like_count FROM likes GROUP BY film_id) AS totals "
                + "ON totals.film_id = f.id "
                + "ORDER BY COALESCE(totals.like_count, 0) DESC, f.id LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        loadFilmRelations(films);
        return films;
    }

    @Override
    @Transactional
    public void deleteFilm(long id) {
        deleteGenres(id);
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ?", id);
        String sql = "DELETE FROM films WHERE id = ?";
        int deletedRows = jdbcTemplate.update(sql, id);

        if (deletedRows == 0) {
            throw new NotFoundException(
                    "Фильм с id " + id + " не найден"
            );
        }
    }

    private Mpa resolveMpa(Mpa mpa) {
        if (mpa == null) {
            return null;
        }
        return mpaDbStorage.getMpa(mpa.getId());
    }

    private void loadFilmRelations(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = new HashMap<>();
        for (Film film : films) {
            filmsById.put(film.getId(), film);
        }

        Map<String, ?> params = Map.of("filmIds", filmsById.keySet());
        String genresSql = SELECT_FILM_GENRES + " WHERE fg.film_id IN (:filmIds) ORDER BY g.id";
        namedJdbcTemplate.query(genresSql, params, resultSet -> {
            Film film = filmsById.get(resultSet.getLong("film_id"));
            film.getGenres().add(mapRowToGenre(resultSet));
        });

        String likesSql = "SELECT film_id, user_id FROM likes WHERE film_id IN (:filmIds)";
        namedJdbcTemplate.query(likesSql, params, resultSet -> {
            Film film = filmsById.get(resultSet.getLong("film_id"));
            film.getLikes().add(resultSet.getLong("user_id"));
        });
    }

    private Set<Long> loadLikes(long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        List<Long> userIds = jdbcTemplate.queryForList(sql, Long.class, filmId);
        return new HashSet<>(userIds);
    }

    private void saveGenres(long filmId, List<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        Set<Integer> genreIds = new LinkedHashSet<>();
        for (Genre genre : genres) {
            if (genre == null || genre.getId() <= 0) {
                throw new ValidationException("У жанра должен быть положительный ID");
            }
            genreIds.add(genre.getId());
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        for (int genreId : genreIds) {
            try {
                jdbcTemplate.update(sql, filmId, genreId);
            } catch (DataIntegrityViolationException e) {
                throw new NotFoundException("Жанр с id " + genreId + " не найден");
            }
        }
    }

    private void deleteGenres(long filmId) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);
    }

    private List<Genre> loadGenres(long filmId) {
        String sql = SELECT_FILM_GENRES + " WHERE fg.film_id = ? ORDER BY g.id";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRowToGenre(resultSet), filmId);
    }

    private Genre mapRowToGenre(ResultSet resultSet) throws SQLException {
        Genre genre = new Genre();
        genre.setId(resultSet.getInt("id"));
        genre.setName(resultSet.getString("name"));
        return genre;
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
