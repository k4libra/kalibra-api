package com.kalibra.api.curriculum.application.internal.eventhandlers;

import com.kalibra.api.curriculum.domain.model.commands.IngestCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.events.MaterialUploaded;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class MaterialUploadedEventHandler {

    private final CurricularMaterialCommandService curricularMaterialCommandService;

    public MaterialUploadedEventHandler(CurricularMaterialCommandService curricularMaterialCommandService) {
        this.curricularMaterialCommandService = curricularMaterialCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MaterialUploaded event) {
        curricularMaterialCommandService.handle(new IngestCurricularMaterialCommand(new MaterialId(event.materialId())));
    }
}
