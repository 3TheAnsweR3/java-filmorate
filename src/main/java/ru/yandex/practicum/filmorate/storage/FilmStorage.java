package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {

    public Film createNewFilm(Film film);

    public Film updateFilm(Film updatedFilm);

    public Film getFilm(long id);

    public List<Film> getAllFilms();

    public void deleteFilm(long id);
}
