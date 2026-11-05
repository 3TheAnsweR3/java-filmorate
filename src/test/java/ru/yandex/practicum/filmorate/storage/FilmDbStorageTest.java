package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(FilmDbStorage.class)
@JdbcTest
@DisplayName("Хранение фильмов в базе данных")
public class FilmDbStorageTest {
    @Autowired
    private FilmDbStorage filmStorage;

    @Test
    @DisplayName("Созданный фильм доступен по ID со всеми сохранёнными полями")
    void shouldCreateAndGetFilmById() {
        Film film = buildFilm(
                "Interstellar",
                "Favorite movie about space",
                LocalDate.of(2014, 10, 26),
                169);

        Film createdFilm = filmStorage.createNewFilm(film);
        assertNotNull(createdFilm);
        assertTrue(createdFilm.getId() > 0);

        Film foundFilm = filmStorage.getFilm(createdFilm.getId());
        assertNotNull(foundFilm);
        assertEquals(createdFilm.getId(), foundFilm.getId());
        assertEquals("Interstellar", foundFilm.getName());
        assertEquals("Favorite movie about space", foundFilm.getDescription());
        assertEquals(LocalDate.of(2014, 10, 26), foundFilm.getReleaseDate());
        assertEquals(169, foundFilm.getDuration());

    }

    @Test
    @DisplayName("Получение несуществующего фильма вызывает NotFoundException")
    void shouldThrowWhenFilmNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> filmStorage.getFilm(999L)
        );
    }

    @Test
    @DisplayName("При отсутствии фильмов возвращается пустой список")
    void shouldReturnEmptyListWhenNoFilms() {
        List<Film> films = filmStorage.getAllFilms();

        assertNotNull(films);
        assertTrue(films.isEmpty());
    }

    @Test
    @DisplayName("Список фильмов содержит все сохранённые фильмы")
    void shouldGetAllFilms() {
        Film firstFilm = buildFilm(
                "Interstellar", "Space",
                LocalDate.of(2014, 10, 26), 169
        );
        Film secondFilm = buildFilm(
                "The Matrix", "Simulation",
                LocalDate.of(1999, 3, 31), 136
        );

        filmStorage.createNewFilm(firstFilm);
        filmStorage.createNewFilm(secondFilm);

        List<Film> films = filmStorage.getAllFilms();

        assertEquals(2, films.size());
        assertTrue(films.stream()
                .anyMatch(film -> film.getId() == firstFilm.getId()));
        assertTrue(films.stream()
                .anyMatch(film -> film.getId() == secondFilm.getId()));
    }

    @Test
    @DisplayName("Обновление фильма сохраняет новые данные без создания новой записи")
    void shouldUpdateFilm() {
        Film film = buildFilm(
                "Old name", "Old description",
                LocalDate.of(2014, 10, 26), 169
        );
        filmStorage.createNewFilm(film);

        Film updatedFilm = buildFilm(
                "New name", "New description",
                LocalDate.of(2015, 1, 10), 170
        );
        updatedFilm.setId(film.getId());

        Film result = filmStorage.updateFilm(updatedFilm);

        assertNotNull(result);
        assertEquals(film.getId(), result.getId());

        Film foundFilm = filmStorage.getFilm(film.getId());

        assertEquals("New name", foundFilm.getName());
        assertEquals("New description", foundFilm.getDescription());
        assertEquals(LocalDate.of(2015, 1, 10), foundFilm.getReleaseDate());
        assertEquals(170, foundFilm.getDuration());
        assertEquals(1, filmStorage.getAllFilms().size());
    }

    @Test
    @DisplayName("Обновление несуществующего фильма вызывает NotFoundException")
    void shouldThrowWhenUpdatingUnknownFilm() {
        Film film = buildFilm(
                "Unknown", "Description",
                LocalDate.of(2014, 10, 26), 169
        );
        film.setId(999L);

        assertThrows(
                NotFoundException.class,
                () -> filmStorage.updateFilm(film)
        );
    }

    @Test
    @DisplayName("Удаление фильма сохраняет остальные фильмы в базе")
    void shouldDeleteFilm() {
        Film film = buildFilm(
                "Interstellar", "Space",
                LocalDate.of(2014, 10, 26), 169
        );
        Film remainingFilm = buildFilm(
                "The Matrix", "Simulation",
                LocalDate.of(1999, 3, 31), 136
        );

        filmStorage.createNewFilm(film);
        filmStorage.createNewFilm(remainingFilm);

        filmStorage.deleteFilm(film.getId());

        assertThrows(
                NotFoundException.class,
                () -> filmStorage.getFilm(film.getId())
        );
        assertEquals(
                remainingFilm.getId(),
                filmStorage.getFilm(remainingFilm.getId()).getId()
        );
        assertEquals(1, filmStorage.getAllFilms().size());
    }

    @Test
    @DisplayName("Удаление несуществующего фильма вызывает NotFoundException")
    void shouldThrowWhenDeletingUnknownFilm() {
        assertThrows(
                NotFoundException.class,
                () -> filmStorage.deleteFilm(999L)
        );
    }

    @Test
    @DisplayName("Рейтинг MPA сохраняется вместе с фильмом и возвращается при чтении")
    void shouldCreateAndGetFilmWithMpa() {
        Film film = buildFilm(
                "Interstellar",
                "Space",
                LocalDate.of(2014, 10, 26),
                169
        );

        Mpa mpa = new Mpa();
        mpa.setId(3);
        mpa.setName("PG-13");
        film.setMpa(mpa);

        Film createdFilm = filmStorage.createNewFilm(film);

        Film foundFilm = filmStorage.getFilm(createdFilm.getId());

        assertNotNull(foundFilm.getMpa());
        assertEquals(3, foundFilm.getMpa().getId());
        assertEquals("PG-13", foundFilm.getMpa().getName());
    }

    @Test
    @DisplayName("Обновление фильма сохраняет новый рейтинг MPA")
    void shouldUpdateFilmMpa() {
        Film film = buildFilm(
                "Interstellar",
                "Space",
                LocalDate.of(2014, 10, 26),
                169
        );

        Mpa initialMpa = new Mpa();
        initialMpa.setId(3);
        initialMpa.setName("PG-13");
        film.setMpa(initialMpa);

        filmStorage.createNewFilm(film);

        Mpa updatedMpa = new Mpa();
        updatedMpa.setId(4);
        updatedMpa.setName("R");
        film.setMpa(updatedMpa);

        filmStorage.updateFilm(film);

        Film foundFilm = filmStorage.getFilm(film.getId());

        assertNotNull(foundFilm.getMpa());
        assertEquals(4, foundFilm.getMpa().getId());
        assertEquals("R", foundFilm.getMpa().getName());
        assertEquals(1, filmStorage.getAllFilms().size());
    }

    private Film buildFilm(String name, String description, LocalDate releaseDate, int duration) {
        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        return film;
    }
}
