package com.jesty.rpchargingseparation;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Simulated GitHub API success, failure, size rejection and recovery (JDK-only). */
public final class UpdaterCheckTest {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }

    public static void main(String[] args) throws Exception {
        long hour = UpdateCheckPolicy.REFRESH_INTERVAL_MS;
        long retry = UpdateCheckPolicy.FAILURE_RETRY_MS;
        long now = 1_000_000L;
        check(UpdateCheckPolicy.shouldCheck(now, 0, 0, false), "first launch");
        check(!UpdateCheckPolicy.shouldCheck(now, now - hour + 1, 0, false), "fresh success");
        check(UpdateCheckPolicy.shouldCheck(now, now - hour, 0, false), "expired success");
        check(!UpdateCheckPolicy.shouldCheck(now, 0, now - retry + 1, false),
                "failure backoff");
        check(UpdateCheckPolicy.shouldCheck(now, 0, now - retry, false), "retry allowed");
        check(UpdateCheckPolicy.shouldCheck(now, now, now, true), "manual bypass");
        check(UpdateCheckPolicy.shouldCheck(now, now + 1, now + 1, false),
                "clock rollback");
        check(!UpdateVersion.isNewer("v1.5.11", "1.5.11"), "same installed version");
        check(UpdateVersion.isNewer("v1.5.11", "1.5.10"), "previous version");

        AtomicInteger responseStatus = new AtomicInteger(200);
        AtomicInteger calls = new AtomicInteger();
        byte[] release = ("{\"tag_name\":\"v1.5.11\",\"assets\":[{" +
                "\"name\":\"Jesty-RP-Charging-Separation-1.5.11.apk\"}]}").getBytes(
                        StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/releases/latest", exchange -> {
            calls.incrementAndGet();
            int status = responseStatus.get();
            byte[] body = status == 200 ? release : "server problem".getBytes(
                    StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (java.io.OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() +
                "/releases/latest";
        try {
            String payload = UpdateHttp.fetchJson(endpoint, 4096);
            check(payload.contains("\"v1.5.11\""), "valid fake GitHub response");
            responseStatus.set(403);
            try {
                UpdateHttp.fetchJson(endpoint, 4096);
                throw new AssertionError("HTTP failure not detected");
            } catch (IOException expected) {
                check(expected.getMessage().contains("403"), "HTTP failure visible");
            }
            responseStatus.set(200);
            try {
                UpdateHttp.fetchJson(endpoint, 8);
                throw new AssertionError("oversize not detected");
            } catch (IOException expected) {
                check(expected.getMessage().contains("too large"), "size guard");
            }
            check(UpdateHttp.fetchJson(endpoint, 4096).equals(payload),
                    "recover after failure");
            check(calls.get() == 4, "one HTTP request per test");
        } finally {
            server.stop(0);
        }
        System.out.println("Updater check integration tests passed");
    }
}
