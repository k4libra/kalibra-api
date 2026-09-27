package com.kalibra.api.curriculum.infrastructure.persistence.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFile;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.CurricularContentEmbeddable;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.CurricularMaterialJpaEntity;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.MaterialFileEmbeddable;
import org.mapstruct.Mapper;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CurricularMaterialJpaMapper {

    CurricularMaterialJpaEntity toEntity(CurricularMaterial curricularMaterial);

    CurricularMaterial toDomain(CurricularMaterialJpaEntity entity);

    MaterialFileEmbeddable toEmbeddable(MaterialFile file);

    MaterialFile toMaterialFile(MaterialFileEmbeddable embeddable);

    CurricularContentEmbeddable toEmbeddable(CurricularContent content);

    CurricularContent toCurricularContent(CurricularContentEmbeddable embeddable);

    // required by MapStruct 1.6: Optional fields and single-field VOs need explicit converters.
    default CurricularContentEmbeddable toContentEmbeddable(Optional<CurricularContent> content) {
        return content == null ? null : content.map(this::toEmbeddable).orElse(null);
    }

    default Optional<CurricularContent> toOptionalContent(CurricularContentEmbeddable embeddable) {
        if (embeddable == null || embeddable.getNormalizedText() == null) {
            return Optional.empty();
        }
        return Optional.of(toCurricularContent(embeddable));
    }

    default String fromOptionalReason(Optional<String> reason) {
        return reason == null ? null : reason.orElse(null);
    }

    default Optional<String> toOptionalReason(String reason) {
        return Optional.ofNullable(reason);
    }

    default UUID map(MaterialId materialId) {
        return materialId == null ? null : materialId.value();
    }

    default MaterialId toMaterialId(UUID value) {
        return value == null ? null : new MaterialId(value);
    }

    default UUID map(CourseId courseId) {
        return courseId == null ? null : courseId.value();
    }

    default CourseId toCourseId(UUID value) {
        return value == null ? null : new CourseId(value);
    }

    default UUID map(SubtopicId subtopicId) {
        return subtopicId == null ? null : subtopicId.value();
    }

    default SubtopicId toSubtopicId(UUID value) {
        return value == null ? null : new SubtopicId(value);
    }
}
