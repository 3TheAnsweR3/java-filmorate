package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class FilmorateApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Полный HTTP-сценарий друзей, лайков и популярности")
    void shouldHandleFriendsLikesAndPopularity() throws Exception {
        long firstUserId = createUser("first");
        long secondUserId = createUser("second");
        long commonUserId = createUser("common");

        mockMvc.perform(put("/users/{id}/friends/{friendId}",
                        firstUserId, commonUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/users/{id}/friends/{friendId}",
                        secondUserId, commonUserId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends/common/{otherId}",
                        firstUserId, secondUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commonUserId));

        long firstFilmId = createFilm("First film");
        long popularFilmId = createFilm("Popular film");

        mockMvc.perform(put("/films/{id}/like/{userId}",
                        firstFilmId, firstUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/like/{userId}",
                        popularFilmId, firstUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/like/{userId}",
                        popularFilmId, secondUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/like/{userId}",
                        popularFilmId, secondUserId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/popular").param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(popularFilmId))
                .andExpect(jsonPath("$[0].likes.length()").value(2));

        mockMvc.perform(delete("/films/{id}/like/{userId}",
                        popularFilmId, secondUserId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/{id}", popularFilmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes.length()").value(1));
    }

    private long createUser(String login) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "email", login + "@example.com",
                "login", login,
                "name", login,
                "birthday", "1990-01-01"
        ));

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private long createFilm(String name) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "name", name,
                "description", "Description",
                "releaseDate", "2000-01-01",
                "duration", 120
        ));

        MvcResult result = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }
}
