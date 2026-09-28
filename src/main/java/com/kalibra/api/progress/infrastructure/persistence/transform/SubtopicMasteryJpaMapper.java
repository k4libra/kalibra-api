package com.kalibra.api.progress.infrastructure.persistence.transform;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.infrastructure.persistence.entities.SubtopicMasteryJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubtopicMasteryJpaMapper {

    SubtopicMasteryJpaEntity toEntity(SubtopicMastery subtopicMastery);

    SubtopicMastery toDomain(SubtopicMasteryJpaEntity entity);
}