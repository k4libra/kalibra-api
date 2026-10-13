package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.AddSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.RemoveSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateSubtopicCommand;

public interface CourseCommandService {

    Course handle(CreateCourseCommand command);
    Course handle(UpdateCourseCommand command);
    Course handle(AddSubtopicCommand command);
    Course handle(UpdateSubtopicCommand command);
    void handle(RemoveSubtopicCommand command);
}
