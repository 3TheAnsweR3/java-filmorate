package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmorateBusinessLogicTests {
    private UserService userService;
    private FilmService filmService;

    @BeforeEach
    void setUp() {
        UserStorage userStorage = new InMemoryUserStorage();
        FilmStorage filmStorage = new InMemoryFilmStorage();
        userService = new UserService(userStorage);
        filmService = new FilmService(filmStorage, userStorage);
    }

    @Test
    @DisplayName("Добавление в друзья должно быть взаимным и не создавать дубликаты")
    void shouldAddFriendSymmetricallyWithoutDuplicates() {
        User first = userService.createUser(createUser("first"));
        User second = userService.createUser(createUser("second"));

        userService.addFriend(first.getId(), second.getId());
        userService.addFriend(first.getId(), second.getId());

        assertEquals(1, first.getFriends().size());
        assertEquals(1, second.getFriends().size());
        assertTrue(first.getFriends().contains(second.getId()));
        assertTrue(second.getFriends().contains(first.getId()));
    }

    @Test
    @DisplayName("Сервис должен возвращать общих друзей")
    void shouldReturnCommonFriends() {
        User first = userService.createUser(createUser("first"));
        User second = userService.createUser(createUser("second"));
        User common = userService.createUser(createUser("common"));

        userService.addFriend(first.getId(), common.getId());
        userService.addFriend(second.getId(), common.getId());

        List<User> result = userService.getCommonFriends(
                first.getId(), second.getId());

        assertEquals(List.of(common), result);
    }

    @Test
    @DisplayName("Повторный лайк не должен увеличивать количество лайков")
    void shouldAddOnlyOneLikeFromUser() {
        User user = userService.createUser(createUser("viewer"));
        Film film = filmService.createNewFilm(createFilm("Film"));

        filmService.addLike(film.getId(), user.getId());
        filmService.addLike(film.getId(), user.getId());

        assertEquals(1, film.getLikes().size());
    }

    @Test
    @DisplayName("Популярные фильмы должны сортироваться по убыванию лайков")
    void shouldReturnFilmsOrderedByLikes() {
        User firstUser = userService.createUser(createUser("first"));
        User secondUser = userService.createUser(createUser("second"));
        Film lessPopular = filmService.createNewFilm(createFilm("Less popular"));
        Film mostPopular = filmService.createNewFilm(createFilm("Most popular"));

        filmService.addLike(lessPopular.getId(), firstUser.getId());
        filmService.addLike(mostPopular.getId(), firstUser.getId());
        filmService.addLike(mostPopular.getId(), secondUser.getId());

        assertEquals(List.of(mostPopular), filmService.getPopular(1));
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }
}
