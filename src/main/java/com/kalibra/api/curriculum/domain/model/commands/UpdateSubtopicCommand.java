package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;

import java.util.Optional;

public record UpdateSubtopicCommand(CourseId courseId, String holderId, SubtopicId subtopicId, Optional<String> name, Optional<Integer> displayOrder, Optional<SubtopicStatus> status) { }
