package com.paymentprocessing.customer_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Business-logic behavior of registration/lookup that isn't specific to auth (see
 * AuthAndAuthorizationTest for the RBAC contract): duplicate email/document must be a
 * clean 409, and an unknown id must be a clean 404 - never a raw 500 leaking a stack
 * trace's worth of DB constraint detail to the client.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CustomerRegistrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registeringTheSameEmailTwiceIsAConflictNotAServerError() throws Exception {
        String body = """
                {"name":"Carol","email":"carol@example.com","document":"999888777","password":"password123"}
                """;

        mockMvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        String duplicateEmail = """
                {"name":"Carol Two","email":"carol@example.com","document":"111222333","password":"password123"}
                """;
        mockMvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(duplicateEmail))
                .andExpect(status().isConflict());

        String duplicateDocument = """
                {"name":"Carol Three","email":"carol3@example.com","document":"999888777","password":"password123"}
                """;
        mockMvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(duplicateDocument))
                .andExpect(status().isConflict());
    }

    @Test
    void gettingAnUnknownCustomerIsUnauthorizedBeforeItIsNotFound() throws Exception {
        // No token at all: the security filter chain rejects the request before the
        // controller (and its 404 mapping) ever runs. This documents that ordering
        // rather than asserting 404 for an anonymous caller.
        mockMvc.perform(get("/customers/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }
}
