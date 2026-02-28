package com.example.kanban.web;

import com.example.kanban.realtime.BoardEventStreamService;
import com.example.kanban.realtime.RealtimeRequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Realtime", description = "Board-scoped SSE stream for realtime collaboration")
@RestController
@RequestMapping("/api/v1/boards/{boardId}/events")
@RequiredArgsConstructor
public class BoardRealtimeController {

    private final BoardEventStreamService boardEventStreamService;
    private final RealtimeRequestContext realtimeRequestContext;

    @Operation(summary = "Subscribe to board events (SSE)")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@boardAccessEvaluator.canRead(#p0, authentication)")
    public SseEmitter subscribe(@PathVariable("boardId") Long boardId,
                                @RequestParam(name = "clientId", required = false) String clientId,
                                @RequestHeader(name = RealtimeRequestContext.CLIENT_ID_HEADER, required = false)
                                String clientIdHeader) {
        String resolvedClientId = realtimeRequestContext.resolveClientId(clientId, clientIdHeader);
        return boardEventStreamService.subscribe(boardId, resolvedClientId);
    }
}
