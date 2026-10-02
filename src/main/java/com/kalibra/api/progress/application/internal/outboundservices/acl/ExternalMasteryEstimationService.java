package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import org.springframework.stereotype.Service;

import java.util.Optional;

// The engine is stateless: with no prior it starts from the base probability, and the
// estimate reports that prior so the first answer also shows a real change.
@Service
public class ExternalMasteryEstimationService {

    public MasteryEstimate estimate(
            Optional<MasteryProbability> prior,
            AnswerResult result) {
        throw new UnsupportedOperationException("Adaptive Engine mastery estimation is wired in engine integration");
    }
}
