package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new LinkedHashMap<>();
    private long id = 0;

    @Override
    public Film createNewFilm(Film film) {
        film.setId(++id);
        films.put(film.getId(), film);
        log.info("Сохранён фильм с id={}, name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film updateFilm(Film updatedFilm) {
        checkFilmExists(updatedFilm.getId());
        films.put(updatedFilm.getId(), updatedFilm);
        log.info("Обновлён фильм с id={}", updatedFilm.getId());
        return updatedFilm;
    }

    @Override
    public Film getFilm(long id) {
        checkFilmExists(id);
        return films.get(id);
    }

    @Override
    public List<Film> getAllFilms() {
        return new ArrayList<>(films.values());
    }

    @Override
    public void deleteFilm(long id) {
        checkFilmExists(id);
        films.remove(id);
        log.info("Удалён фильм с id={}", id);
    }

    private void checkFilmExists(long id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
    }
}
