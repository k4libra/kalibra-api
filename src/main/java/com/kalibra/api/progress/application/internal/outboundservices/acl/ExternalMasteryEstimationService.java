package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.shared.engine.EngineUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Optional;

// The engine is stateless: with no prior it starts from the base probability, and the
// estimate reports that prior so the first answer also shows a real change.
// Synchronous on purpose: the student sees the change right after answering.
@Service
public class ExternalMasteryEstimationService {

    private static final String MASTERY_ESTIMATES = "/api/v1/mastery-estimates";

    private final RestClient engineRestClient;

    public ExternalMasteryEstimationService(@Qualifier("engineRestClient") RestClient engineRestClient) {
        this.engineRestClient = engineRestClient;
    }

    public MasteryEstimate estimate(
            String holderId,
            SubtopicId subtopicId,
            Optional<MasteryProbability> prior,
            AnswerResult result) {
        var request = new HashMap<String, Object>();
        request.put("studentId", holderId);
        request.put("subtopicId", subtopicId.value().toString());
        request.put("priorProbability", prior.map(MasteryProbability::value).orElse(null));
        request.put("outcome", result.name());
        JsonNode response;
        try {
            response = engineRestClient.post()
                    .uri(MASTERY_ESTIMATES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException failure) {
            throw new EngineUnavailableException("The adaptive engine could not estimate the mastery", failure);
        }
        if (response == null || !response.hasNonNull("posterior") || !response.hasNonNull("level")) {
            throw new EngineUnavailableException("The adaptive engine answered an incomplete mastery estimate");
        }
        return new MasteryEstimate(
                new MasteryProbability(response.get("posterior").asDouble()),
                MasteryLevel.valueOf(response.get("level").asText()),
                response.hasNonNull("prior")
                        ? Optional.of(new MasteryProbability(response.get("prior").asDouble()))
                        : Optional.empty(),
                response.path("initializedFromBase").asBoolean());
    }
}
