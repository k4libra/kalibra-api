package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeneratedExercisePoolRefillService {

    private static final Logger log = LoggerFactory.getLogger(GeneratedExercisePoolRefillService.class);

    private final GeneratedExercisePoolRefillTask task;
    private final Set<RefillKey> inFlight = ConcurrentHashMap.newKeySet();

    private record RefillKey(CourseId courseId, SubtopicId subtopicId, DifficultyLevel difficulty) { }

    public GeneratedExercisePoolRefillService(GeneratedExercisePoolRefillTask task) {
        this.task = task;
    }

    public void request(GenerateExerciseForStudentCommand command, CurricularContent context) {
        var key = new RefillKey(command.courseId(), command.subtopicId(), command.targetDifficulty().orElseThrow());
        if (!inFlight.add(key)) {
            return;
        }
        try {
            task.refill(command, context).whenComplete((ignored, failure) -> {
                inFlight.remove(key);
                if (failure != null) {
                    log.warn("Exercise pool refill failed for {}", key, failure);
                }
            });
        } catch (Exception failure) {
            inFlight.remove(key);
            log.warn("Exercise pool refill could not be scheduled for {}", key, failure);
        }
    }
}
