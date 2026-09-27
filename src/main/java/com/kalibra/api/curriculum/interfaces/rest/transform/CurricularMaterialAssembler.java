package com.kalibra.api.curriculum.interfaces.rest.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularMaterialPage;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.interfaces.rest.resources.CurricularMaterialPageResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.CurricularMaterialResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.UploadCurricularMaterialResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CurricularMaterialAssembler {

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "subtopicIds", source = "resource.subtopicIds")
    @Mapping(target = "fileName", source = "resource.fileName")
    @Mapping(target = "format", source = "resource.format")
    @Mapping(target = "content", source = "content")
    UploadCurricularMaterialCommand toCommand(UploadCurricularMaterialResource resource,
                                              UUID courseId,
                                              String holderId,
                                              byte[] content);

    @Mapping(target = "fileName", source = "file.fileName")
    @Mapping(target = "format", source = "file.format")
    CurricularMaterialResource toResource(CurricularMaterial material);

    @Mapping(target = "content", source = "items")
    CurricularMaterialPageResource toResource(CurricularMaterialPage page);

    // required by MapStruct: VOs, Optional fields and the format need explicit converters.
    default MaterialFormat toFormat(String value) {
        return MaterialFormat.from(value);
    }

    default CourseId toCourseId(UUID value) {
        return value == null ? null : new CourseId(value);
    }

    default SubtopicId toSubtopicId(UUID value) {
        return value == null ? null : new SubtopicId(value);
    }

    default UUID map(CourseId courseId) {
        return courseId == null ? null : courseId.value();
    }

    default UUID map(SubtopicId subtopicId) {
        return subtopicId == null ? null : subtopicId.value();
    }

    default UUID map(MaterialId materialId) {
        return materialId == null ? null : materialId.value();
    }

    default String map(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }
}
