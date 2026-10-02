package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;

import java.util.Optional;

public class ExternalMasteryEstimationService {

    public MasteryEstimate estimate(
            Optional<MasteryProbability> prior,
            AnswerResult result) {
        return null;
    }
}