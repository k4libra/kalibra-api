package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.util.List;

public record UploadCurricularMaterialCommand(
        String holderId,
        CourseId courseId,
        List<SubtopicId> subtopicIds,
        String fileName,
        MaterialFormat format,
        byte[] content
) { }
