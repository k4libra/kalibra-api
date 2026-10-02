package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalEngineServicesTest {

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final CurricularContent context = new CurricularContent("Linear equations", 2);

    @Test
    void shouldFailClearlyWhileTheGenerationEngineIsNotWired() {
        var service = new ExternalExerciseGenerationService();

        assertThatThrownBy(() -> service.generate(new GenerateExercisesForSubtopicCommand("teacher-1", courseId, subtopicId, 1), context))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("engine integration");
        assertThatThrownBy(() -> service.generate(new GenerateExerciseForStudentCommand(courseId, subtopicId, Optional.empty()), context))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("engine integration");
    }

    @Test
    void shouldFailWhileTheExtractionEngineIsNotWired() {
        assertThatThrownBy(() -> new ExternalCurricularExtractionService().extract(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
