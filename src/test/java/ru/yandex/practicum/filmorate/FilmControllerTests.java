package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTests {

    private FilmController filmController;

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void createFilmController() {
        filmController = new FilmController();
    }

    @ParameterizedTest
    @DisplayName("Аннотационная валидация должна отклонять некорректное название фильма")
    @NullAndEmptySource
    @ValueSource(strings = "     ")
    void shouldDetectInvalidName(String name) {
        Film film = createValidFilm();
        film.setName(name);

        assertFalse(validator.validate(film).isEmpty());
    }

    @Test
    @DisplayName("Должно выбрасываться исключение, если описание фильма длиннее 200 символов")
    void shouldDetectInvalidDescription() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(201));

        assertFalse(validator.validate(film).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Фильм с допустимой длиной описания должен успешно создаваться")
    @ValueSource(ints = {199, 200})
    void shouldCreateFilmWithValidDescriptionLength(int descriptionLength) {
        Film film = createValidFilm();
        film.setDescription("a".repeat(descriptionLength));

        assertTrue(validator.validate(film).isEmpty());
    }

    @Test
    @DisplayName("Должно выбрасываться исключение, если дата релиза раньше 28 декабря 1895 года")
    void shouldThrowExceptionForInvalidReleaseDate() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class,
                () -> filmController.createNewFilm(film));
    }

    @ParameterizedTest
    @DisplayName("Фильм с допустимой датой релиза должен успешно создаваться")
    @ValueSource(strings = {"1895-12-28", "1895-12-29"})
    void shouldCreateFilmWithValidReleaseDate(String releaseDate) {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.parse(releaseDate));

        assertDoesNotThrow(() -> filmController.createNewFilm(film));
    }

    @ParameterizedTest
    @DisplayName("Должно выбрасываться исключение, если продолжительность фильма меньше или равна нулю")
    @ValueSource(ints = {-1, 0})
    void shouldDetectInvalidDuration(int duration) {
        Film film = createValidFilm();
        film.setDuration(duration);

        assertFalse(validator.validate(film).isEmpty());
    }

    @Test
    @DisplayName("Валидный фильм должен успешно создаваться")
    void shouldCreateValidFilm() {
        Film film = createValidFilm();

        assertDoesNotThrow(() -> filmController.createNewFilm(film));
    }

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Interstellar");
        film.setDescription("Favorite movie about space!");
        film.setReleaseDate(LocalDate.of(2014, 11, 5));
        film.setDuration(1); // для проверки пограничного значения

        return film;
    }
}
