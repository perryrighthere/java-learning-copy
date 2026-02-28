package com.example.kanban.web;

import com.example.kanban.domain.Board;
import com.example.kanban.repository.BoardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week3CoreCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BoardRepository boardRepository;

    private String adminToken;
    private Long boardId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = accessTokenFor("alice@kanban.local", "Password123!");
        Board board = boardRepository.findFirstByName("Week 2 Demo Board")
            .orElseThrow();
        boardId = board.getId();
    }

    @Test
    void boardCrudShouldSupportPaginationSearchAndSoftDelete() throws Exception {
        String boardName = "Week3 Search Board";

        String createResponse = mockMvc.perform(post("/api/v1/boards")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBoardRequest(boardName))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(boardName))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long createdBoardId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/boards")
                .header("Authorization", "Bearer " + adminToken)
                .param("q", "Search Board")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").isNumber())
            .andExpect(jsonPath("$.items[0].name").value(boardName));

        mockMvc.perform(delete("/api/v1/boards/{id}", createdBoardId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/boards/{id}", createdBoardId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void columnCardAndCommentCrudShouldRespectSoftDeleteFiltering() throws Exception {
        Long columnId = createColumn(boardId, "Week3 Temp Column");
        Long cardId = createCard(columnId, "Week3 Temp Card", "Temp description");
        Long commentId = createComment(cardId, "Temp comment");

        mockMvc.perform(delete("/api/v1/cards/{cardId}/comments/{commentId}", cardId, commentId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cards/{cardId}/comments", cardId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(delete("/api/v1/columns/{columnId}/cards/{cardId}", columnId, cardId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/columns/{columnId}/cards", columnId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(delete("/api/v1/boards/{boardId}/columns/{columnId}", boardId, columnId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/boards/{boardId}/columns/{columnId}", boardId, columnId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void cardUpdateShouldReturnConflictWhenVersionIsStale() throws Exception {
        Long columnId = createColumn(boardId, "Week3 Lock Column");
        Long cardId = createCard(columnId, "Lock Card", "First version");

        String currentCardBody = mockMvc.perform(get("/api/v1/columns/{columnId}/cards/{cardId}", columnId, cardId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode currentCard = objectMapper.readTree(currentCardBody);
        int version = currentCard.get("version").asInt();

        UpdateCardRequest firstUpdate = new UpdateCardRequest("Lock Card Updated", "After first update", null, version);
        String updatedBody = mockMvc.perform(put("/api/v1/columns/{columnId}/cards/{cardId}", columnId, cardId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUpdate)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int latestVersion = objectMapper.readTree(updatedBody).get("version").asInt();
        assertThat(latestVersion).isGreaterThan(version);

        UpdateCardRequest staleUpdate = new UpdateCardRequest("Stale Attempt", "Should fail", null, version);
        mockMvc.perform(put("/api/v1/columns/{columnId}/cards/{cardId}", columnId, cardId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(staleUpdate)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.latest.id").value(cardId))
            .andExpect(jsonPath("$.latest.version").value(latestVersion));
    }

    private Long createColumn(Long boardId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/boards/{boardId}/columns", boardId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateColumnRequest(name, null))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createCard(Long columnId, String title, String description) throws Exception {
        String response = mockMvc.perform(post("/api/v1/columns/{columnId}/cards", columnId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCardRequest(title, description, null))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createComment(Long cardId, String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/cards/{cardId}/comments", cardId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCommentRequest(body))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
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

    private record CreateBoardRequest(String name) {
    }

    private record CreateColumnRequest(String name, Object position) {
    }

    private record CreateCardRequest(String title, String description, Object position) {
    }

    private record CreateCommentRequest(String body) {
    }

    private record UpdateCardRequest(String title, String description, Object position, Integer version) {
    }
}
