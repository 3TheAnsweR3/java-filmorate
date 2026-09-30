package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserControllerTests {

    private UserController userController;

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void createUserController() {
        userController = new UserController();
    }

    @ParameterizedTest
    @DisplayName("Должно выбрасываться исключение при некорректном email")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ivan#xmail.com", "ivanxmail.com"})
    void shouldDetectInvalidEmail(String email) {
        User user = createValidUser();
        user.setEmail(email);

        assertFalse(validator.validate(user).isEmpty());
    }

    @Test
    @DisplayName("Валидный пользователь должен успешно создаваться")
    void shouldCreateValidUser() {
        User user = createValidUser();

        assertDoesNotThrow(() -> userController.createUser(user));
        assertEquals("Ivan", user.getName());
    }

    @ParameterizedTest
    @DisplayName("Аннотационная валидация должна отклонять пустой логин")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldDetectBlankLogin(String login) {
        User user = createValidUser();
        user.setLogin(login);

        assertFalse(validator.validate(user).isEmpty());
    }

    @Test
    @DisplayName("Должно выбрасываться исключение, если логин содержит пробел")
    void shouldThrowExceptionForLoginWithSpaces() {
        User user = createValidUser();
        user.setLogin("i van");

        assertThrows(ValidationException.class,
                () -> userController.createUser(user));
    }

    @ParameterizedTest
    @DisplayName("Если имя пользователя пустое, должен использоваться логин")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldCreateUserNameFromLogin(String name) {
        User user = createValidUser();
        user.setName(name);

        userController.createUser(user);

        assertEquals(user.getLogin(), user.getName());
    }

    @ParameterizedTest
    @DisplayName("Пользователь с датой рождения сегодня или в прошлом должен успешно создаваться")
    @ValueSource(ints = {-1, 0})
    void shouldCreateUserWithValidBirthday(int dayOffset) {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(dayOffset));

        assertTrue(validator.validate(user).isEmpty());
    }

    @Test
    @DisplayName("Должно выбрасываться исключение, если дата рождения находится в будущем")
    void shouldThrowExceptionForFutureBirthday() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertFalse(validator.validate(user).isEmpty());
    }

    private User createValidUser() {
        User user = new User();
        user.setEmail("ivan@xmail.com");
        user.setLogin("ivan");
        user.setName("Ivan");
        user.setBirthday(LocalDate.of(1993, 9, 12));

        return user;
    }

}
