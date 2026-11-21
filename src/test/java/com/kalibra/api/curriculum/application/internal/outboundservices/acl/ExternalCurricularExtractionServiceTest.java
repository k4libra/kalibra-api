package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.kalibra.api.curriculum.application.internal.outboundservices.storage.MaterialStorageService;
import com.kalibra.api.shared.engine.EngineTaskClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalCurricularExtractionServiceTest {

    private static final Instant START = Instant.parse("2026-11-16T10:00:00Z");
    private static final String CAPABILITIES_URL = "http://engine/api/v1/capabilities";

    private final Clock clock = mock(Clock.class);
    private MockRestServiceServer server;
    private ExternalCurricularExtractionService service;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://engine");
        server = MockRestServiceServer.bindTo(builder).build();
        when(clock.instant()).thenReturn(START);
        service = new ExternalCurricularExtractionService(mock(EngineTaskClient.class),
                mock(MaterialStorageService.class), builder.build(), Duration.ofSeconds(30), clock);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    private void expectAvailability(boolean available) {
        server.expect(requestTo(CAPABILITIES_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"curricularExtraction\":" + available
                        + ",\"exerciseGeneration\":" + !available + "}", MediaType.APPLICATION_JSON));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnExtractionAvailabilityWhenEngineAnswers(boolean available) {
        expectAvailability(available);

        assertThat(service.isCurricularExtractionAvailable()).isEqualTo(available);
    }

    @Test
    void shouldReturnFalseWhenEngineIsUnreachable() {
        server.expect(requestTo(CAPABILITIES_URL)).andRespond(withException(new IOException("connection refused")));

        assertThat(service.isCurricularExtractionAvailable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 404, 500, 503})
    void shouldReturnFalseWhenEngineAnswersUnexpectedStatus(int status) {
        server.expect(requestTo(CAPABILITIES_URL))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"curricularExtraction\":true}"));

        assertThat(service.isCurricularExtractionAvailable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{\"curricularExtraction\":\"true\"}", "not json"})
    void shouldReturnFalseWhenEngineResponseIsInvalid(String body) {
        server.expect(requestTo(CAPABILITIES_URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThat(service.isCurricularExtractionAvailable()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenEngineResponseIsEmpty() {
        server.expect(requestTo(CAPABILITIES_URL)).andRespond(withSuccess());

        assertThat(service.isCurricularExtractionAvailable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReuseCachedAvailabilityWhenTtlHasNotExpired(boolean available) {
        expectAvailability(available);

        assertThat(service.isCurricularExtractionAvailable()).isEqualTo(available);
        when(clock.instant()).thenReturn(START.plusSeconds(29));
        assertThat(service.isCurricularExtractionAvailable()).isEqualTo(available);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRefreshAvailabilityWhenTtlExpires(boolean initialAvailability) {
        expectAvailability(initialAvailability);
        expectAvailability(!initialAvailability);

        assertThat(service.isCurricularExtractionAvailable()).isEqualTo(initialAvailability);
        when(clock.instant()).thenReturn(START.plusSeconds(30));
        assertThat(service.isCurricularExtractionAvailable()).isEqualTo(!initialAvailability);
    }

    @Test
    void shouldRecoverWhenFailedLookupExpires() {
        server.expect(requestTo(CAPABILITIES_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        expectAvailability(true);

        assertThat(service.isCurricularExtractionAvailable()).isFalse();
        when(clock.instant()).thenReturn(START.plusSeconds(29));
        assertThat(service.isCurricularExtractionAvailable()).isFalse();
        when(clock.instant()).thenReturn(START.plusSeconds(30));
        assertThat(service.isCurricularExtractionAvailable()).isTrue();
    }

    @Test
    void shouldDiscardCachedTrueWhenRefreshFails() {
        expectAvailability(true);
        server.expect(requestTo(CAPABILITIES_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(service.isCurricularExtractionAvailable()).isTrue();
        when(clock.instant()).thenReturn(START.plusSeconds(30));
        assertThat(service.isCurricularExtractionAvailable()).isFalse();
    }

    @Test
    void shouldShareOneLookupWhenRequestsAreConcurrent() throws Exception {
        expectAvailability(true);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var requests = IntStream.range(0, 8)
                    .mapToObj(index -> (Callable<Boolean>) service::isCurricularExtractionAvailable)
                    .toList();
            for (var result : executor.invokeAll(requests)) {
                assertThat(result.get()).isTrue();
            }
        }
    }
}
