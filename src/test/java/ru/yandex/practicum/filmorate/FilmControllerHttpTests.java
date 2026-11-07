package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:film-http-tests")
@AutoConfigureMockMvc
class FilmControllerHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Создание, обновление и чтение фильма возвращают жанры и MPA из справочников")
    void shouldReturnStoredGenresAndMpa() throws Exception {
        ObjectNode request = filmRequest(3, List.of(Map.of("id", 2), Map.of("id", 4)));

        MvcResult created = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres.length()").value(2))
                .andExpect(jsonPath("$.genres[0].id").value(2))
                .andExpect(jsonPath("$.genres[1].id").value(4))
                .andExpect(jsonPath("$.mpa.id").value(3))
                .andExpect(jsonPath("$.mpa.name").value("PG-13"))
                .andReturn();
        long filmId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        ObjectNode update = filmRequest(4, List.of(Map.of("id", 6)));
        update.put("id", filmId);
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mpa.name").value("R"));

        mockMvc.perform(get("/films/{id}", filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres.length()").value(1))
                .andExpect(jsonPath("$.genres[0].id").value(6))
                .andExpect(jsonPath("$.genres[0].name").value("Боевик"))
                .andExpect(jsonPath("$.mpa.id").value(4))
                .andExpect(jsonPath("$.mpa.name").value("R"));
    }

    @Test
    @DisplayName("Создание фильма с неизвестным рейтингом возвращает 404")
    void shouldRejectUnknownMpa() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmRequest(999, List.of()).toString()))
                .andExpect(status().isNotFound());
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

    private ObjectNode filmRequest(int mpaId, List<Map<String, Integer>> genres) {
        return objectMapper.valueToTree(Map.of(
                "name", "Genre test film",
                "description", "Film with genres",
                "releaseDate", "2014-11-05",
                "duration", 169,
                "genres", genres,
                "mpa", Map.of("id", mpaId)
        ));
    }
}
