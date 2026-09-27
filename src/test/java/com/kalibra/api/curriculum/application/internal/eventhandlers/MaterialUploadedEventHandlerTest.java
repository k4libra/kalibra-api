package com.kalibra.api.curriculum.application.internal.eventhandlers;

import com.kalibra.api.curriculum.domain.model.commands.IngestCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.events.MaterialUploaded;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MaterialUploadedEventHandlerTest {

    @Mock
    CurricularMaterialCommandService curricularMaterialCommandService;

    @InjectMocks
    MaterialUploadedEventHandler handler;

    @Test
    void shouldAskToIngestTheUploadedMaterial() {
        var materialId = UUID.randomUUID();

        handler.on(new MaterialUploaded(materialId));

        verify(curricularMaterialCommandService).handle(new IngestCurricularMaterialCommand(new MaterialId(materialId)));
    }
}
