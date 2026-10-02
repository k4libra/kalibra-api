package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.shared.engine.EngineUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadGateway;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalMasteryEstimationServiceTest {

    private final String holderId = UUID.randomUUID().toString();
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());

    private MockRestServiceServer engine;
    private ExternalMasteryEstimationService service;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://engine");
        engine = MockRestServiceServer.bindTo(builder).build();
        service = new ExternalMasteryEstimationService(builder.build());
    }

    @Test
    void shouldSendThePriorAndTranslateTheEstimateOfTheEngine() {
        engine.expect(requestTo("http://engine/api/v1/mastery-estimates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.studentId").value(holderId))
                .andExpect(jsonPath("$.subtopicId").value(subtopicId.value().toString()))
                .andExpect(jsonPath("$.priorProbability").value(0.42))
                .andExpect(jsonPath("$.outcome").value("CORRECT"))
                .andRespond(withSuccess(
                        "{\"prior\":0.42,\"posterior\":0.75,\"level\":\"HIGH\",\"initializedFromBase\":false}",
                        MediaType.APPLICATION_JSON));

        var estimate = service.estimate(holderId, subtopicId, Optional.of(new MasteryProbability(0.42)), AnswerResult.CORRECT);

        assertThat(estimate.probability().value()).isEqualTo(0.75);
        assertThat(estimate.level()).isEqualTo(MasteryLevel.HIGH);
        assertThat(estimate.prior()).contains(new MasteryProbability(0.42));
        assertThat(estimate.initializedFromBase()).isFalse();
    }

    @Test
    void shouldLetTheEngineStartFromItsBaseOnTheFirstAnswer() {
        engine.expect(requestTo("http://engine/api/v1/mastery-estimates"))
                .andExpect(jsonPath("$.priorProbability").doesNotExist())
                .andRespond(withSuccess(
                        "{\"prior\":0.30,\"posterior\":0.19,\"level\":\"LOW\",\"initializedFromBase\":true}",
                        MediaType.APPLICATION_JSON));

        var estimate = service.estimate(holderId, subtopicId, Optional.empty(), AnswerResult.INCORRECT);

        assertThat(estimate.prior()).contains(new MasteryProbability(0.30));
        assertThat(estimate.initializedFromBase()).isTrue();
    }

    @Test
    void shouldReportTheEngineAsUnavailableWhenItFails() {
        engine.expect(requestTo("http://engine/api/v1/mastery-estimates")).andRespond(withBadGateway());

        assertThatThrownBy(() -> service.estimate(holderId, subtopicId, Optional.empty(), AnswerResult.CORRECT))
                .isInstanceOf(EngineUnavailableException.class);
    }
}
