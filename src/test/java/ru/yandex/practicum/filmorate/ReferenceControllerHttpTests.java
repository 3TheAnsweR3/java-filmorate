package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reference-http-tests;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@DisplayName("HTTP-доступ к справочникам жанров и рейтингов")
class ReferenceControllerHttpTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /genres возвращает шесть жанров")
    void shouldGetAllGenres() throws Exception {
        mockMvc.perform(get("/genres"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Комедия"))
                .andExpect(jsonPath("$[5].id").value(6))
                .andExpect(jsonPath("$[5].name").value("Боевик"));
    }

    @Test
    @DisplayName("GET /genres/{id} возвращает выбранный жанр")
    void shouldGetGenreById() throws Exception {
        mockMvc.perform(get("/genres/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Драма"));
    }

    @Test
    @DisplayName("GET /genres/{id} возвращает 404 для неизвестного жанра")
    void shouldReturnNotFoundForUnknownGenre() throws Exception {
        mockMvc.perform(get("/genres/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("GET /mpa возвращает пять возрастных рейтингов")
    void shouldGetAllMpa() throws Exception {
        mockMvc.perform(get("/mpa"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("G"))
                .andExpect(jsonPath("$[4].id").value(5))
                .andExpect(jsonPath("$[4].name").value("NC-17"));
    }

    @Test
    @DisplayName("GET /mpa/{id} возвращает выбранный рейтинг")
    void shouldGetMpaById() throws Exception {
        mockMvc.perform(get("/mpa/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("PG-13"));
    }

    @Test
    @DisplayName("GET /mpa/{id} возвращает 404 для неизвестного рейтинга")
    void shouldReturnNotFoundForUnknownMpa() throws Exception {
        mockMvc.perform(get("/mpa/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}