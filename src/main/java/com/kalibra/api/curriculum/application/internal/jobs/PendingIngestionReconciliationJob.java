package com.kalibra.api.curriculum.application.internal.jobs;

import com.kalibra.api.curriculum.domain.model.commands.IngestCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetPendingIngestionMaterialsQuery;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialCommandService;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingIngestionReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(PendingIngestionReconciliationJob.class);

    private final CurricularMaterialQueryService curricularMaterialQueryService;
    private final CurricularMaterialCommandService curricularMaterialCommandService;

    public PendingIngestionReconciliationJob(CurricularMaterialQueryService curricularMaterialQueryService,
                                              CurricularMaterialCommandService curricularMaterialCommandService) {
        this.curricularMaterialQueryService = curricularMaterialQueryService;
        this.curricularMaterialCommandService = curricularMaterialCommandService;
    }

    @Scheduled(fixedDelayString = "${kalibra.curriculum.ingestion-reconciliation-delay:PT5M}",
            initialDelayString = "${kalibra.curriculum.ingestion-reconciliation-delay:PT5M}")
    public void run() {
        for (var material : curricularMaterialQueryService.handle(new GetPendingIngestionMaterialsQuery())) {
            try {
                curricularMaterialCommandService.handle(new IngestCurricularMaterialCommand(material.getId()));
            } catch (RuntimeException failure) {
                log.error("Ingestion retry failed for curricular material {}", material.getId().value(), failure);
            }
        }
    }
}
