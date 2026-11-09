package com.mottu.ops.infrastructure.adapter.out.client;

import com.mottu.ops.application.dto.SyncCatalogResponseDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class IntelligenceServiceClientTest {

    private static final String TOKEN = "test-token";
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private String startServer(int status, String body, AtomicReference<String> upgradeHeader, AtomicReference<String> receivedBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/catalog/sync", exchange -> {
            upgradeHeader.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    @DisplayName("UC-11: sync sends plain HTTP/1.1 body (no h2c upgrade) and returns live result")
    void syncCatalogUsesHttp11AndReturnsLiveResult() throws Exception {
        AtomicReference<String> upgrade = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String url = startServer(200, "{\"syncedCount\":1297,\"usedFallback\":false}", upgrade, body);

        SyncCatalogResponseDto result = new IntelligenceServiceClient(url, TOKEN).syncCatalog();

        assertNull(upgrade.get(), "uvicorn drops POST bodies sent with an h2c Upgrade header");
        assertTrue(body.get().contains("\"forceRefresh\":true"));
        assertEquals(1297, result.syncedCount());
        assertFalse(result.usedFallback());
    }

    @Test
    @DisplayName("UC-11: sync failure is reported honestly (0 parts, fallback flag, reason in message)")
    void syncCatalogFailureIsReportedHonestly() throws Exception {
        String url = startServer(500, "{\"detail\":\"boom\"}", new AtomicReference<>(), new AtomicReference<>());

        SyncCatalogResponseDto result = new IntelligenceServiceClient(url, TOKEN).syncCatalog();

        assertTrue(result.usedFallback());
        assertEquals(0, result.syncedCount());
        assertTrue(result.message().contains("500"), result.message());
    }
}

