package com.example.kanban.web;

import com.example.kanban.domain.Board;
import com.example.kanban.realtime.RealtimeRequestContext;
import com.example.kanban.repository.BoardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class Week4RealtimeIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("app.realtime.redis-enabled", () -> true);
        registry.add("app.realtime.redis-channel-prefix", () -> "kanban:test-week4");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BoardRepository boardRepository;

    @LocalServerPort
    private int port;

    private String adminToken;
    private Long boardId;
    private Long columnId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = accessTokenFor("alice@kanban.local", "Password123!");
        Board board = boardRepository.findFirstByName("Week 2 Demo Board").orElseThrow();
        boardId = board.getId();
        columnId = createColumn(boardId, "Week4 Realtime Column " + UUID.randomUUID());
    }

    @Test
    void shouldRelayRedisBoardEventsToSecondClientAndSuppressPublisherEcho() throws Exception {
        try (SseStream clientA = SseStream.connect(port, boardId, "client-a", adminToken);
             SseStream clientB = SseStream.connect(port, boardId, "client-b", adminToken)) {

            assertThat(clientA.awaitEvent("connected", Duration.ofSeconds(5))).isNotNull();
            assertThat(clientB.awaitEvent("connected", Duration.ofSeconds(5))).isNotNull();

            mockMvc.perform(post("/api/v1/columns/{columnId}/cards", columnId)
                    .header("Authorization", "Bearer " + adminToken)
                    .header(RealtimeRequestContext.CLIENT_ID_HEADER, "client-a")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new CreateCardRequest("Week4 Realtime Card", "Created by client A", null))))
                .andExpect(status().isOk());

            SseEnvelope eventForB = clientB.awaitEvent("card.created", Duration.ofSeconds(10));
            assertThat(eventForB).isNotNull();

            JsonNode payload = objectMapper.readTree(eventForB.data());
            assertThat(payload.get("boardId").asLong()).isEqualTo(boardId);
            assertThat(payload.get("eventType").asText()).isEqualTo("card.created");
            assertThat(payload.get("resourceType").asText()).isEqualTo("card");
            assertThat(payload.get("originClientId").asText()).isEqualTo("client-a");
            assertThat(payload.get("actorUserId").asLong()).isPositive();
            assertThat(payload.get("resourceVersion").asLong()).isPositive();

            assertThat(clientA.awaitEvent("card.created", Duration.ofSeconds(2))).isNull();
        }
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

    private record CreateColumnRequest(String name, Object position) {
    }

    private record CreateCardRequest(String title, String description, Object position) {
    }

    private record SseEnvelope(String id, String eventName, String data) {
    }

    private static final class SseStream implements AutoCloseable {

        private final BlockingQueue<SseEnvelope> events = new LinkedBlockingQueue<>();
        private final java.util.concurrent.ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
        private final InputStream inputStream;
        private final CompletableFuture<Void> readerTask;

        private SseStream(InputStream inputStream) {
            this.inputStream = inputStream;
            this.readerTask = CompletableFuture.runAsync(this::readLoop, readerExecutor);
        }

        static SseStream connect(int port, Long boardId, String clientId, String token) throws Exception {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/api/v1/boards/%d/events?clientId=%s".formatted(port, boardId, clientId)))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .GET()
                .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertThat(response.statusCode()).isEqualTo(200);
            return new SseStream(response.body());
        }

        SseEnvelope awaitEvent(String expectedEventName, Duration timeout) throws InterruptedException {
            long timeoutNanos = timeout.toNanos();
            long deadline = System.nanoTime() + timeoutNanos;
            while (System.nanoTime() < deadline) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return null;
                }
                SseEnvelope next = events.poll(remaining, TimeUnit.NANOSECONDS);
                if (next == null) {
                    return null;
                }
                if (expectedEventName.equals(next.eventName())) {
                    return next;
                }
            }
            return null;
        }

        private void readLoop() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                String id = null;
                String eventName = "message";
                StringBuilder data = new StringBuilder();
                while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        if (data.length() > 0 || eventName != null) {
                            events.offer(new SseEnvelope(id, eventName, data.toString()));
                        }
                        id = null;
                        eventName = "message";
                        data = new StringBuilder();
                        continue;
                    }
                    if (line.startsWith("id:")) {
                        id = line.substring(3).trim();
                    } else if (line.startsWith("event:")) {
                        eventName = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        if (data.length() > 0) {
                            data.append('\n');
                        }
                        data.append(line.substring(5).trim());
                    }
                }
            } catch (IOException ignored) {
            }
        }

        @Override
        public void close() throws Exception {
            inputStream.close();
            readerTask.cancel(true);
            readerExecutor.shutdownNow();
        }
    }
}
