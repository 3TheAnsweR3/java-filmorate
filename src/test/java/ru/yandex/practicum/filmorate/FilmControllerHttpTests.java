package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilmControllerHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /films принимает идентификаторы жанров и рейтинга")
    void shouldAcceptGenreAndMpaIds() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "name", "Genre test film",
                "description", "Film with two genres",
                "releaseDate", "2014-11-05",
                "duration", 169,
                "genres", List.of(Map.of("id", 2), Map.of("id", 4)),
                "mpa", Map.of("id", 3)
        ));

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres.length()").value(2))
                .andExpect(jsonPath("$.genres[0].id").value(2))
                .andExpect(jsonPath("$.genres[1].id").value(4))
                .andExpect(jsonPath("$.mpa.id").value(3));
    }

    @Test
    @DisplayName("POST /films без тела должен возвращать 400 Bad Request")
    void shouldReturnBadRequestWhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /films/{id} для отсутствующего фильма должен возвращать 404")
    void shouldReturnNotFoundForUnknownFilm() throws Exception {
        mockMvc.perform(get("/films/999"))
                .andExpect(status().isNotFound());
    }
}
