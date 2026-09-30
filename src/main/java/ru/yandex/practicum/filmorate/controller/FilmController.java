package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final List<Film> films = new ArrayList<>();
    private int id = 0;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @PostMapping
    public Film createNewFilm(@Valid @RequestBody Film film) {
        validateReleaseDate(film);

        film.setId(++id);
        films.add(film);

        log.info("Создан фильм с id={}, name={}",
                film.getId(),
                film.getName());

        return film;
    }

    @GetMapping
    public List<Film> getAllFilms() {
        return films;
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film updatedFilm) {
        validateReleaseDate(updatedFilm);

        for (int i = 0; i < films.size(); i++) {
            if (films.get(i).getId() == updatedFilm.getId()) {
                films.set(i, updatedFilm);

                log.info("Обновлён фильм с id={}", updatedFilm.getId());

                return updatedFilm;
            }
        }
        log.warn("Не удалось обновить фильм: id={} не найден",
                updatedFilm.getId());
        throw new ValidationException("Фильм с id " + updatedFilm.getId() + " не найден!");
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации фильма: дата релиза {} раньше допустимой {}",
                    film.getReleaseDate(),
                    MIN_RELEASE_DATE);
            throw new ValidationException(
                    "Дата релиза фильма не должна быть ранее 28.12.1895!");
        }
    }
}
