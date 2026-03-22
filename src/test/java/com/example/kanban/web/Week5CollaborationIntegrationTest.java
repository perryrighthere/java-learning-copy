package com.example.kanban.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week5CollaborationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = accessTokenFor("alice@kanban.local", "Password123!");
    }

    @Test
    void columnReorderShouldPersistRequestedOrderTransactionally() throws Exception {
        Long boardId = createBoard("Week5 Reorder Board " + UUID.randomUUID());
        Long backlogId = createColumn(boardId, "Backlog");
        Long doingId = createColumn(boardId, "Doing");
        Long reviewId = createColumn(boardId, "Review");

        String body = mockMvc.perform(patch("/api/v1/boards/{boardId}/columns/reorder", boardId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ReorderColumnsRequest(List.of(reviewId, backlogId, doingId)))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode response = objectMapper.readTree(body).get("columns");
        assertThat(response).hasSize(3);
        assertThat(response.get(0).get("id").asLong()).isEqualTo(reviewId);
        assertThat(response.get(1).get("id").asLong()).isEqualTo(backlogId);
        assertThat(response.get(2).get("id").asLong()).isEqualTo(doingId);
        assertThat(new BigDecimal(response.get(0).get("position").asText())).isEqualByComparingTo("100.00");
        assertThat(new BigDecimal(response.get(1).get("position").asText())).isEqualByComparingTo("200.00");
        assertThat(new BigDecimal(response.get(2).get("position").asText())).isEqualByComparingTo("300.00");

        mockMvc.perform(get("/api/v1/boards/{boardId}/columns", boardId)
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(reviewId))
            .andExpect(jsonPath("$.items[1].id").value(backlogId))
            .andExpect(jsonPath("$.items[2].id").value(doingId));
    }

    @Test
    void cardMoveShouldPlaceCardBetweenRequestedNeighbors() throws Exception {
        Long boardId = createBoard("Week5 Move Board " + UUID.randomUUID());
        Long sourceColumnId = createColumn(boardId, "Source");
        Long targetColumnId = createColumn(boardId, "Target");

        CardSnapshot movingCard = createCard(sourceColumnId, "Move Me", "Week5 drag/drop source");
        CardSnapshot firstTarget = createCard(targetColumnId, "Target A", "First target");
        CardSnapshot secondTarget = createCard(targetColumnId, "Target B", "Second target");

        String body = mockMvc.perform(patch("/api/v1/cards/{cardId}/move", movingCard.id())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MoveCardRequest(targetColumnId, firstTarget.id(), secondTarget.id(), movingCard.version()))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode movedCard = objectMapper.readTree(body);
        assertThat(movedCard.get("columnId").asLong()).isEqualTo(targetColumnId);
        assertThat(new BigDecimal(movedCard.get("position").asText())).isEqualByComparingTo("150.00");
        assertThat(movedCard.get("version").asInt()).isGreaterThan(movingCard.version());

        mockMvc.perform(get("/api/v1/columns/{columnId}/cards", targetColumnId)
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(firstTarget.id()))
            .andExpect(jsonPath("$.items[1].id").value(movingCard.id()))
            .andExpect(jsonPath("$.items[2].id").value(secondTarget.id()));

        mockMvc.perform(get("/api/v1/columns/{columnId}/cards", sourceColumnId)
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void cardMoveShouldReturnConflictWithGuidanceWhenVersionIsStale() throws Exception {
        Long boardId = createBoard("Week5 Conflict Board " + UUID.randomUUID());
        Long sourceColumnId = createColumn(boardId, "Source");
        Long targetColumnId = createColumn(boardId, "Target");

        CardSnapshot movingCard = createCard(sourceColumnId, "Conflict Card", "Original");
        String updateBody = mockMvc.perform(put("/api/v1/columns/{columnId}/cards/{cardId}", sourceColumnId, movingCard.id())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UpdateCardRequest("Conflict Card Updated", "New version", null, movingCard.version()))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int latestVersion = objectMapper.readTree(updateBody).get("version").asInt();

        mockMvc.perform(patch("/api/v1/cards/{cardId}/move", movingCard.id())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MoveCardRequest(targetColumnId, null, null, movingCard.version()))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("optimistic_lock_conflict"))
            .andExpect(jsonPath("$.retryable").value(true))
            .andExpect(jsonPath("$.guidance[0]").exists())
            .andExpect(jsonPath("$.latest.id").value(movingCard.id()))
            .andExpect(jsonPath("$.latest.version").value(latestVersion));
    }

    @Test
    void cardMoveShouldReturnConflictWhenNeighborOrderIsStale() throws Exception {
        Long boardId = createBoard("Week5 Neighbor Conflict Board " + UUID.randomUUID());
        Long sourceColumnId = createColumn(boardId, "Source");
        Long targetColumnId = createColumn(boardId, "Target");

        CardSnapshot movingCard = createCard(sourceColumnId, "Drag Me", "Will hit ordering conflict");
        CardSnapshot firstTarget = createCard(targetColumnId, "Target A", "First target");
        createCard(targetColumnId, "Target B", "Middle target");
        CardSnapshot thirdTarget = createCard(targetColumnId, "Target C", "Third target");

        mockMvc.perform(patch("/api/v1/cards/{cardId}/move", movingCard.id())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MoveCardRequest(targetColumnId, firstTarget.id(), thirdTarget.id(), movingCard.version()))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("ordering_conflict"))
            .andExpect(jsonPath("$.retryable").value(true))
            .andExpect(jsonPath("$.guidance[0]").exists())
            .andExpect(jsonPath("$.latest.id").value(movingCard.id()));
    }

    private Long createBoard(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/boards")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBoardRequest(name))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
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

    private CardSnapshot createCard(Long columnId, String title, String description) throws Exception {
        String response = mockMvc.perform(post("/api/v1/columns/{columnId}/cards", columnId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCardRequest(title, description, null))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode card = objectMapper.readTree(response);
        return new CardSnapshot(
            card.get("id").asLong(),
            card.get("columnId").asLong(),
            card.get("version").asInt()
        );
    }

    private String accessTokenFor(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
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

    private record UpdateCardRequest(String title, String description, Object position, Integer version) {
    }

    private record MoveCardRequest(Long targetColumnId, Long previousCardId, Long nextCardId, Integer version) {
    }

    private record ReorderColumnsRequest(List<Long> orderedColumnIds) {
    }

    private record CardSnapshot(Long id, Long columnId, Integer version) {
    }
}
