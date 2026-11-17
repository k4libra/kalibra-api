package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.kalibra.api.curriculum.application.internal.outboundservices.storage.MaterialStorageService;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.shared.engine.EngineProperties;
import com.kalibra.api.shared.engine.EngineTaskClient;
import com.kalibra.api.shared.engine.EngineTaskFailedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ExternalCurricularExtractionService {

    static final String TASK_TYPE = "curricular-extractions";

    private final EngineTaskClient engineTaskClient;
    private final MaterialStorageService materialStorageService;
    private final RestClient engineRestClient;
    private final Duration capabilitiesTtl;
    private final Clock clock;
    private Instant capabilitiesExpireAt;
    private boolean curricularExtractionAvailable;

    @Autowired
    public ExternalCurricularExtractionService(EngineTaskClient engineTaskClient,
                                               MaterialStorageService materialStorageService,
                                               @Qualifier("engineRestClient") RestClient engineRestClient,
                                               EngineProperties properties) {
        this(engineTaskClient, materialStorageService, engineRestClient, properties.capabilitiesTtl(), Clock.systemUTC());
    }

    ExternalCurricularExtractionService(EngineTaskClient engineTaskClient,
                                        MaterialStorageService materialStorageService,
                                        RestClient engineRestClient,
                                        Duration capabilitiesTtl,
                                        Clock clock) {
        this.engineTaskClient = engineTaskClient;
        this.materialStorageService = materialStorageService;
        this.engineRestClient = engineRestClient;
        this.capabilitiesTtl = capabilitiesTtl;
        this.clock = clock;
    }

    public synchronized boolean isCurricularExtractionAvailable() {
        var now = clock.instant();
        if (capabilitiesExpireAt != null && now.isBefore(capabilitiesExpireAt)) {
            return curricularExtractionAvailable;
        }
        curricularExtractionAvailable = false;
        try {
            var capabilities = engineRestClient.get()
                    .uri("/api/v1/capabilities")
                    .retrieve()
                    .onStatus(status -> status.value() != 200, (request, response) -> {
                        throw new RestClientException("Material upload availability is temporarily unavailable");
                    })
                    .body(JsonNode.class);
            if (capabilities != null) {
                var extraction = capabilities.path("curricularExtraction");
                curricularExtractionAvailable = extraction.isBoolean() && extraction.booleanValue();
            }
        } catch (RestClientException unavailable) {
            curricularExtractionAvailable = false;
        }
        capabilitiesExpireAt = now.plus(capabilitiesTtl);
        return curricularExtractionAvailable;
    }

    public Optional<CurricularContent> extract(CurricularMaterial material) {
        var payload = Map.of(
                "materialId", material.getId().value().toString(),
                "storageReference", materialStorageService.temporaryUrl(material.getFile().storageReference()),
                "format", material.getFile().format().name().toLowerCase(Locale.ROOT));
        try {
            var result = engineTaskClient.submit(TASK_TYPE, payload);
            return Optional.of(new CurricularContent(
                    result.path("normalizedText").asText(),
                    result.path("pageCount").asInt()));
        } catch (EngineTaskFailedException failed) {
            if (failed.isRejectedInput()) {
                return Optional.empty();
            }
            throw failed;
        }
    }
}
