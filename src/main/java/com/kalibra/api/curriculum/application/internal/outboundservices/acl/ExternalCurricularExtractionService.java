package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExternalCurricularExtractionService {

    public Optional<CurricularContent> extract(CurricularMaterial material) {
        throw new UnsupportedOperationException("Adaptive Engine extraction is not available yet");
    }
}
