package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface FilmStorage {

    public Film createNewFilm(Film film);

    public Film updateFilm(Film updatedFilm);

    public Film getFilm(long id);

    public List<Film> getAllFilms();

    public void deleteFilm(long id);

    void addLike(Film film, User user);

    void removeLike(Film film, User user);

    List<Film> getPopular(int count);
}
