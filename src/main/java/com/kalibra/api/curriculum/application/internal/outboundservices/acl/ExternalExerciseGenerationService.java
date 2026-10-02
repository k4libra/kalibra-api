package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import org.springframework.stereotype.Service;

import java.util.List;

// Returns every attempt of the engine, approved or discarded: the caller persists them all and
// only hands an approved one to a student.
@Service
public class ExternalExerciseGenerationService {

    public List<GeneratedExercise> generate(GenerateExerciseForStudentCommand command, CurricularContent context) {
        throw new UnsupportedOperationException("Adaptive Engine exercise generation is wired in engine integration");
    }

    public List<GeneratedExercise> generate(GenerateExercisesForSubtopicCommand command, CurricularContent context) {
        throw new UnsupportedOperationException("Adaptive Engine exercise generation is wired in engine integration");
    }
}
