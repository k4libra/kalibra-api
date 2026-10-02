package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.kalibra.api.curriculum.application.internal.outboundservices.storage.MaterialStorageService;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.shared.engine.EngineTaskClient;
import com.kalibra.api.shared.engine.EngineTaskFailedException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Extraction runs on the engine's task queue. Empty means the engine rejected the material itself
// (unreadable or unsupported): it becomes an ingestion error. Anything else (engine or provider
// down, missing key, timeout) is thrown, so the material stays pending and is retried later.
@Service
public class ExternalCurricularExtractionService {

    static final String TASK_TYPE = "curricular-extractions";

    private final EngineTaskClient engineTaskClient;
    private final MaterialStorageService materialStorageService;

    public ExternalCurricularExtractionService(EngineTaskClient engineTaskClient,
                                               MaterialStorageService materialStorageService) {
        this.engineTaskClient = engineTaskClient;
        this.materialStorageService = materialStorageService;
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
