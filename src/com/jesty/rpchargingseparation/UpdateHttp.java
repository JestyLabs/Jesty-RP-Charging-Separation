package com.jesty.rpchargingseparation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Bounded GitHub release-feed transport, exercised with a local fake server. */
final class UpdateHttp {
    private UpdateHttp() { }

    static String fetchJson(String url, int maxBytes) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "Jesty-RP-Charging-Separation-Updater");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + status);
            if (!"https".equals(connection.getURL().getProtocol())
                    && !url.startsWith("http://127.0.0.1:")) {
                throw new IOException("release feed was redirected away from HTTPS");
            }
            long declared = connection.getContentLengthLong();
            if (declared > maxBytes) throw new IOException("release response is too large");
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (result.size() > maxBytes - count) {
                        throw new IOException("release response is too large");
                    }
                    result.write(buffer, 0, count);
                }
                return new String(result.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
