package com.kalibra.api.shared.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EngineTaskClientTest {

    private MockRestServiceServer server;
    private EngineTaskClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://engine");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new EngineTaskClient(builder.build(), new ObjectMapper());
    }

    @Test
    void postsThePayloadToTheUseCaseEndpointAndReturnsItsBody() {
        server.expect(requestTo("http://engine/api/v1/curricular-extractions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"materialId\":\"m-1\"}"))
                .andRespond(withSuccess("{\"normalizedText\":\"texto\",\"pageCount\":3}", MediaType.APPLICATION_JSON));

        var result = client.submit("curricular-extractions", Map.of("materialId", "m-1"));

        assertThat(result.path("pageCount").asInt()).isEqualTo(3);
        server.verify();
    }

    @Test
    void shouldKeepRejectedInputStatusWhenEngineDetailsAreSanitized() {
        server.expect(requestTo("http://engine/api/v1/curricular-extractions"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":422,\"detail\":\"Unreadable material\"}"));

        assertThatThrownBy(() -> client.submit("curricular-extractions", Map.of()))
                .isInstanceOfSatisfying(EngineTaskFailedException.class, failed -> {
                    assertThat(failed.isRejectedInput()).isTrue();
                    assertThat(failed.getMessage()).isEqualTo("The adaptive engine task failed");
                });
    }

    @Test
    void shouldUseGenericMessageWhenEngineFailureContainsInternalDetails() {
        server.expect(requestTo("http://engine/api/v1/curricular-extractions"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":502,\"detail\":\"Mistral API key configuration unavailable\"}"));

        assertThatThrownBy(() -> client.submit("curricular-extractions", Map.of()))
                .isInstanceOfSatisfying(EngineTaskFailedException.class, failed -> {
                    assertThat(failed.getStatus()).isEqualTo(502);
                    assertThat(failed.getMessage()).isEqualTo("The adaptive engine task failed");
                });
        server.verify();
    }

    @Test
    void reportsAProviderFailureWithItsStatus() {
        server.expect(requestTo("http://engine/api/v1/exercise-generations"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("not json"));

        assertThatThrownBy(() -> client.submit("exercise-generations", Map.of()))
                .isInstanceOfSatisfying(EngineTaskFailedException.class, failed -> {
                    assertThat(failed.getStatus()).isEqualTo(502);
                    assertThat(failed.isRejectedInput()).isFalse();
                });
    }

    @Test
    void treatsAnUnreachableEngineAsUnavailable() {
        server.expect(requestTo("http://engine/api/v1/exercise-generations"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.submit("exercise-generations", Map.of()))
                .isInstanceOf(EngineUnavailableException.class);
    }
}
