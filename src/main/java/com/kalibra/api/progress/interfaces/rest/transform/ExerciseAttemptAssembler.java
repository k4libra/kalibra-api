package com.kalibra.api.progress.interfaces.rest.transform;

import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.interfaces.rest.resources.PracticeExerciseResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExerciseAttemptAssembler {

    PracticeExerciseResource toResource(PracticeExerciseView view);
}