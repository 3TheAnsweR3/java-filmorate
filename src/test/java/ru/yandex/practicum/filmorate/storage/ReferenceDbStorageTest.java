package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import({GenreDbStorage.class, MpaDbStorage.class})
@DisplayName("Справочники жанров и возрастных рейтингов")
class ReferenceDbStorageTest {
    @Autowired
    private GenreDbStorage genreStorage;

    @Autowired
    private MpaDbStorage mpaStorage;

    @Test
    @DisplayName("Справочник содержит шесть жанров с установленными ID")
    void shouldGetAllGenres() {
        List<Genre> genres = genreStorage.getAllGenres();

        assertEquals(
                List.of(1, 2, 3, 4, 5, 6),
                genres.stream().map(Genre::getId).toList()
        );
        assertEquals(
                List.of(
                        "Комедия", "Драма", "Мультфильм",
                        "Триллер", "Документальный", "Боевик"
                ),
                genres.stream().map(Genre::getName).toList()
        );
    }

    @Test
    @DisplayName("Жанр доступен по ID")
    void shouldGetGenreById() {
        Genre genre = genreStorage.getGenre(1);

        assertNotNull(genre);
        assertEquals(1, genre.getId());
        assertEquals("Комедия", genre.getName());
    }

    @Test
    @DisplayName("Несуществующий жанр вызывает NotFoundException")
    void shouldThrowWhenGenreNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> genreStorage.getGenre(999)
        );
    }

    @Test
    @DisplayName("Справочник содержит пять рейтингов с установленными ID")
    void shouldGetAllMpa() {
        List<Mpa> ratings = mpaStorage.getAllMpa();

        assertEquals(
                List.of(1, 2, 3, 4, 5),
                ratings.stream().map(Mpa::getId).toList()
        );
        assertEquals(
                List.of("G", "PG", "PG-13", "R", "NC-17"),
                ratings.stream().map(Mpa::getName).toList()
        );
    }

    @Test
    @DisplayName("Возрастной рейтинг доступен по ID")
    void shouldGetMpaById() {
        Mpa mpa = mpaStorage.getMpa(3);

        assertNotNull(mpa);
        assertEquals(3, mpa.getId());
        assertEquals("PG-13", mpa.getName());
    }

    @Test
    @DisplayName("Несуществующий рейтинг вызывает NotFoundException")
    void shouldThrowWhenMpaNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> mpaStorage.getMpa(999)
        );
    }
}