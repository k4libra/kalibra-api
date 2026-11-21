package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.IngestCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;

public interface CurricularMaterialCommandService {

    CurricularMaterial handle(UploadCurricularMaterialCommand command);

    void handle(IngestCurricularMaterialCommand command);
}
