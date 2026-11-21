package com.kalibra.api.shared.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

// Long-running engine use cases (extraction, generation) over the engine's REST contract: one
// request, one response, so any API instance gets its own answer. A problem response from the
// engine keeps its status (422 = bad input, do not retry; 502 = provider failed, retry later);
// a timeout or an unreachable engine is "unavailable", so callers keep the work pending and the
// reconciliation job retries it. Callers must be safe to repeat.
@Component
public class EngineTaskClient {

    private final RestClient engineTaskRestClient;
    private final ObjectMapper objectMapper;

    public EngineTaskClient(@Qualifier("engineTaskRestClient") RestClient engineTaskRestClient,
                            ObjectMapper objectMapper) {
        this.engineTaskRestClient = engineTaskRestClient;
        this.objectMapper = objectMapper;
    }

    // Returns the engine's REST response body for the use case (for example "curricular-extractions").
    public JsonNode submit(String type, Object payload) {
        try {
            return engineTaskRestClient.post()
                    .uri("/api/v1/{type}", type)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw failureOf(response);
                    })
                    .body(JsonNode.class);
        } catch (EngineTaskFailedException failed) {
            throw failed;
        } catch (RestClientException unreachable) {
            throw new EngineUnavailableException("The adaptive engine did not answer the task " + type, unreachable);
        }
    }

    private EngineTaskFailedException failureOf(ClientHttpResponse response) throws IOException {
        var status = response.getStatusCode().value();
        var problem = readProblem(response);
        return new EngineTaskFailedException(
                problem.path("status").asInt(status),
                "The adaptive engine task failed");
    }

    private JsonNode readProblem(ClientHttpResponse response) {
        try (var body = response.getBody()) {
            return objectMapper.readTree(body);
        } catch (IOException | RuntimeException unreadable) {
            return objectMapper.createObjectNode();
        }
    }
}
