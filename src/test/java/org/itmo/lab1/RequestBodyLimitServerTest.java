package org.itmo.lab1;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "test"})
class RequestBodyLimitServerTest {

    private static final byte[] OVERSIZED = ("{\"username\":\"alice\",\"password\":\"" + "p".repeat(20 * 1024) + "\"}")
            .getBytes(StandardCharsets.UTF_8);

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @LocalServerPort
    private int port;

    @Test
    void oversizedBodyWithDeclaredLengthGets413() throws Exception {
        assertThat(login(HttpRequest.BodyPublishers.ofByteArray(OVERSIZED)).statusCode()).isEqualTo(413);
    }

    @Test
    void oversizedChunkedBodyGets413() throws Exception {
        HttpRequest.BodyPublisher chunked =
                HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(OVERSIZED));

        HttpResponse<String> response = login(chunked);

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("\"status\":413").doesNotContain("pppp");
    }

    private HttpResponse<String> login(HttpRequest.BodyPublisher body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(body)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
