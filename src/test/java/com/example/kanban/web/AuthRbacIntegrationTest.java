package com.example.kanban.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.kanban.domain.Board;
import com.example.kanban.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BoardRepository boardRepository;

    private Long boardId;

    @BeforeEach
    void setUp() {
        Board board = boardRepository.findFirstByName("Week 2 Demo Board")
            .orElseThrow();
        boardId = board.getId();
    }

    @Test
    void loginAndRefreshShouldReturnJwtPair() throws Exception {
        String loginBody = """
            {
              "email": "alice@kanban.local",
              "password": "Password123!"
            }
            """;

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String refreshToken = loginJson.get("refreshToken").asText();

        String refreshBody = objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken));

        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String refreshedAccessToken = objectMapper.readTree(refreshResponse).get("accessToken").asText();
        assertThat(refreshedAccessToken).isNotBlank();
    }

    @Test
    void boardShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/boards/{id}", boardId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void guestCanReadBoardButCannotManageMembers() throws Exception {
        String guestToken = accessTokenFor("guest@kanban.local", "Password123!");

        mockMvc.perform(get("/api/v1/boards/{id}", boardId)
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk());

        String addMemberBody = """
            {
              "userId": 2,
              "role": "MEMBER"
            }
            """;

        mockMvc.perform(post("/api/v1/boards/{boardId}/members", boardId)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addMemberBody))
            .andExpect(status().isForbidden());
    }

    private String accessTokenFor(String email, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, password));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private record LoginRequest(String email, String password) {
    }

    private record RefreshTokenRequest(String refreshToken) {
    }
}
