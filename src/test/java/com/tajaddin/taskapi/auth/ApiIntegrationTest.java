package com.tajaddin.taskapi.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end flow through the full Spring MVC + security filter chain, backed
 * by H2 in PostgreSQL mode with the real Flyway migrations applied. No Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    // Instantiate directly; Spring Boot 4 does not expose a default ObjectMapper bean.
    private final ObjectMapper json = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123"}""".formatted(email);
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("token").asText();
    }

    @Test
    void protectedRouteRejectsAnonymous() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerLoginAndCrudTask() throws Exception {
        String token = registerAndGetToken("crud@example.com");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"crud@example.com","password":"password123"}"""))
                .andExpect(status().isOk());

        String created = mvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Ship the API","description":"before Friday"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Ship the API")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(get("/api/tasks/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/tasks/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/tasks/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeEachOthersTasks() throws Exception {
        String alice = registerAndGetToken("alice@example.com");
        String bob = registerAndGetToken("bob@example.com");

        String created = mvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Alice secret"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long aliceTaskId = json.readTree(created).get("id").asLong();

        // Bob gets 404 (not 403) so existence is not leaked.
        mvc.perform(get("/api/tasks/" + aliceTaskId).header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        registerAndGetToken("dup@example.com");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@example.com","password":"password123"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validationRejectsShortPassword() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"short@example.com","password":"abc"}"""))
                .andExpect(status().isBadRequest());
    }
}
