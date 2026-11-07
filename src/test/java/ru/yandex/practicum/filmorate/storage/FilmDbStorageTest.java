package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Import({FilmDbStorage.class, UserDbStorage.class, MpaDbStorage.class})
@JdbcTest
@DisplayName("Хранение фильмов в базе данных")
public class FilmDbStorageTest {
    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

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

        film.setGenres(List.of(genre(2)));
        filmStorage.createNewFilm(film);
        remainingFilm.setGenres(List.of(genre(4)));
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

    @Test
    @DisplayName("Создание фильма сохраняет разные жанры, а чтение берёт названия из справочника")
    void shouldCreateAndGetFilmWithGenres() {
        Film film = buildFilm("Interstellar", "Space", LocalDate.of(2014, 10, 26), 169);
        film.setGenres(List.of(genre(4), genre(2), genre(2)));

        Film createdFilm = filmStorage.createNewFilm(film);
        Film foundFilm = filmStorage.getFilm(createdFilm.getId());

        assertEquals(List.of(2, 4), foundFilm.getGenres().stream().map(Genre::getId).toList());
        assertEquals(List.of("Драма", "Триллер"), foundFilm.getGenres().stream().map(Genre::getName).toList());
        assertEquals(foundFilm.getGenres(), createdFilm.getGenres());

        Film anotherFilm = buildFilm("The Matrix", "Simulation", LocalDate.of(1999, 3, 31), 136);
        anotherFilm.setGenres(List.of(genre(6)));
        filmStorage.createNewFilm(anotherFilm);
        List<Film> films = filmStorage.getAllFilms();
        assertEquals(foundFilm.getGenres(), films.get(0).getGenres());
        assertEquals(List.of(6), films.get(1).getGenres().stream().map(Genre::getId).toList());
    }

    @Test
    @DisplayName("Обновление заменяет жанры фильма, а пустой список удаляет все связи")
    void shouldReplaceAndClearFilmGenres() {
        Film film = buildFilm("Interstellar", "Space", LocalDate.of(2014, 10, 26), 169);
        film.setGenres(List.of(genre(2), genre(4)));
        filmStorage.createNewFilm(film);

        film.setGenres(List.of(genre(4), genre(6)));
        Film updatedFilm = filmStorage.updateFilm(film);
        List<Genre> genres = filmStorage.getFilm(film.getId()).getGenres();
        assertEquals(List.of(4, 6), genres.stream().map(Genre::getId).toList());
        assertEquals(genres, updatedFilm.getGenres());

        film.setGenres(List.of());
        filmStorage.updateFilm(film);
        assertTrue(filmStorage.getFilm(film.getId()).getGenres().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Неизвестный жанр отменяет обновление фильма и сохраняет прежние связи")
    void shouldKeepStoredFilmWhenGenreIsUnknown() {
        Film film = buildFilm("Interstellar", "Space", LocalDate.of(2014, 10, 26), 169);
        film.setGenres(List.of(genre(2)));
        filmStorage.createNewFilm(film);

        try {
            film.setName("New name");
            film.setGenres(List.of(genre(4), genre(999)));
            assertThrows(NotFoundException.class, () -> filmStorage.updateFilm(film));

            Film foundFilm = filmStorage.getFilm(film.getId());
            assertEquals("Interstellar", foundFilm.getName());
            assertEquals(List.of(2), foundFilm.getGenres().stream().map(Genre::getId).toList());
        } finally {
            filmStorage.deleteFilm(film.getId());
        }
    }

    @Test
    @DisplayName("Повторный лайк не дублируется, а удаление убирает только выбранный лайк")
    void shouldAddAndRemoveLikes() {
        Film film = filmStorage.createNewFilm(
                buildFilm("Interstellar", "Space", LocalDate.of(2014, 10, 26), 169));
        User firstUser = createUser("first");
        User secondUser = createUser("second");

        filmStorage.addLike(film.getId(), firstUser.getId());
        filmStorage.addLike(film.getId(), firstUser.getId());
        filmStorage.addLike(film.getId(), secondUser.getId());
        assertEquals(Set.of(firstUser.getId(), secondUser.getId()), filmStorage.getFilm(film.getId()).getLikes());

        film.setLikes(Set.of());
        assertEquals(Set.of(firstUser.getId(), secondUser.getId()), filmStorage.updateFilm(film).getLikes());

        filmStorage.removeLike(film.getId(), firstUser.getId());
        filmStorage.removeLike(film.getId(), firstUser.getId());
        assertEquals(Set.of(secondUser.getId()), filmStorage.getAllFilms().getFirst().getLikes());
    }

    @Test
    @DisplayName("Популярность учитывает число лайков, а при равенстве фильмы идут по ID")
    void shouldOrderPopularFilmsByLikeCount() {
        Film first = filmStorage.createNewFilm(
                buildFilm("First", "No likes", LocalDate.of(2014, 10, 26), 169));
        Film second = filmStorage.createNewFilm(
                buildFilm("Second", "One like", LocalDate.of(2014, 10, 26), 169));
        Film third = filmStorage.createNewFilm(
                buildFilm("Third", "Two likes", LocalDate.of(2014, 10, 26), 169));
        Film fourth = filmStorage.createNewFilm(
                buildFilm("Fourth", "One like", LocalDate.of(2014, 10, 26), 169));
        User firstUser = createUser("first");
        User secondUser = createUser("second");

        third.setGenres(List.of(genre(2)));
        filmStorage.updateFilm(third);
        filmStorage.addLike(second.getId(), firstUser.getId());
        filmStorage.addLike(third.getId(), firstUser.getId());
        filmStorage.addLike(third.getId(), secondUser.getId());
        filmStorage.addLike(fourth.getId(), secondUser.getId());

        List<Film> popular = filmStorage.getPopular(2);
        assertEquals(List.of(third.getId(), second.getId()), popular.stream().map(Film::getId).toList());
        assertEquals(Set.of(firstUser.getId(), secondUser.getId()), popular.getFirst().getLikes());
        assertEquals(List.of(2), popular.getFirst().getGenres().stream().map(Genre::getId).toList());
        assertEquals(List.of(third.getId(), second.getId(), fourth.getId(), first.getId()),
                filmStorage.getPopular(10).stream().map(Film::getId).toList());
        assertTrue(filmStorage.getPopular(0).isEmpty());
    }

    @Test
    @DisplayName("Удаление фильма и пользователя убирает их лайки, сохраняя остальные")
    void shouldDeleteOnlyRelatedLikes() {
        Film deleted = filmStorage.createNewFilm(
                buildFilm("Interstellar", "Space", LocalDate.of(2014, 10, 26), 169));
        Film remaining = filmStorage.createNewFilm(
                buildFilm("The Matrix", "Simulation", LocalDate.of(1999, 3, 31), 136));
        User firstUser = createUser("first");
        User secondUser = createUser("second");

        filmStorage.addLike(deleted.getId(), firstUser.getId());
        filmStorage.addLike(remaining.getId(), firstUser.getId());
        filmStorage.addLike(remaining.getId(), secondUser.getId());

        filmStorage.deleteFilm(deleted.getId());
        assertThrows(NotFoundException.class, () -> filmStorage.getFilm(deleted.getId()));
        assertEquals(Set.of(firstUser.getId(), secondUser.getId()), filmStorage.getFilm(remaining.getId()).getLikes());

        userStorage.deleteUser(firstUser.getId());
        assertThrows(NotFoundException.class, () -> userStorage.getUser(firstUser.getId()));
        assertEquals(Set.of(secondUser.getId()), filmStorage.getFilm(remaining.getId()).getLikes());
    }

    private User createUser(String login) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.com");
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.createNewUser(user);
    }

    private Genre genre(int id) {
        Genre genre = new Genre();
        genre.setId(id);
        return genre;
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
