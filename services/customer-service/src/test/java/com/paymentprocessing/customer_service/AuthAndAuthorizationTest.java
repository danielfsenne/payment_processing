package com.paymentprocessing.customer_service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Proves the RBAC/distributed-authorization contract end to end through real HTTP + a
 * real JWT (not a mocked SecurityContext): a customer can read their own record, gets
 * 403 reading someone else's, and the seeded ADMIN account can read and list everyone.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndAuthorizationTest {

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

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Test
    void customersOnlyAccessTheirOwnRecordWhileAdminSeesEverything() throws Exception {
        String idA = register("Alice", "alice@example.com", "111", "password123");
        String tokenA = login("alice@example.com", "password123");

        String idB = register("Bob", "bob@example.com", "222", "password123");
        String tokenB = login("bob@example.com", "password123");

        String adminToken = login(adminEmail, adminPassword);

        mockMvc.perform(get("/customers/{id}", idA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customers/{id}", idA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/customers/{id}", idA).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customers").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/customers").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customers/{id}", idB))
                .andExpect(status().isUnauthorized());
    }

    private String register(String name, String email, String document, String password) throws Exception {
        String requestBody = """
                {"name":"%s","email":"%s","document":"%s","password":"%s"}
                """.formatted(name, email, document, password);
        String response = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String login(String email, String password) throws Exception {
        String requestBody = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
