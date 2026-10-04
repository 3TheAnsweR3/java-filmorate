package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film createNewFilm(Film film) {
        validateFilm(film);
        film.setLikes(new HashSet<>());
        return filmStorage.createNewFilm(film);
    }

    public Film updateFilm(Film updatedFilm) {
        validateFilm(updatedFilm);
        Film storedFilm = filmStorage.getFilm(updatedFilm.getId());
        updatedFilm.setLikes(storedFilm.getLikes());
        return filmStorage.updateFilm(updatedFilm);
    }

    public Film getFilm(long id) {
        return filmStorage.getFilm(id);
    }

    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public void deleteFilm(long id) {
        filmStorage.deleteFilm(id);
    }

    public void addLike(long filmId, long userId) {
        Film film = filmStorage.getFilm(filmId);
        userStorage.getUser(userId);

        if (film.getLikes().add(userId)) {
            filmStorage.updateFilm(film);
            log.info("Пользователь с id={} поставил лайк фильму с id={}",
                    userId, filmId);
        }
    }

    public void removeLike(long filmId, long userId) {
        Film film = filmStorage.getFilm(filmId);
        userStorage.getUser(userId);

        if (film.getLikes().remove(userId)) {
            filmStorage.updateFilm(film);
            log.info("Пользователь с id={} удалил лайк у фильма с id={}",
                    userId, filmId);
        }
    }

    public List<Film> getPopular(int count) {
        if (count < 0) {
            throw new ValidationException(
                    "Количество популярных фильмов не может быть отрицательным");
        }

        return filmStorage.getAllFilms()
                .stream()
                .sorted(Comparator.comparingInt(
                                (Film film) -> film.getLikes().size())
                        .reversed())
                .limit(count)
                .toList();
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate() != null
                && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException(
                    "Дата релиза фильма не должна быть ранее 28.12.1895");
        }
    }
}
