package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.CurricularMaterialJpaEntity;
import com.kalibra.api.curriculum.infrastructure.persistence.transform.CurricularMaterialJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurricularMaterialRepositoryImplTest {

    @Mock
    CurricularMaterialJpaRepository jpaRepository;

    @Mock
    CurricularMaterialJpaMapper mapper;

    @InjectMocks
    CurricularMaterialRepositoryImpl repository;

    private final CourseId courseId = new CourseId(UUID.randomUUID());

    private CurricularMaterial material() {
        var command = new UploadCurricularMaterialCommand("teacher-1", courseId,
                List.of(new SubtopicId(UUID.randomUUID())), "unit.pdf", MaterialFormat.PDF, new byte[]{1});
        return CurricularMaterial.register(command, "ref.pdf");
    }

    @Test
    void shouldMapSaveAndMapBackWhenSaving() {
        // Arrange
        var material = material();
        var entity = new CurricularMaterialJpaEntity();
        when(mapper.toEntity(material)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(material);

        // Act & Assert
        assertThat(repository.save(material)).isEqualTo(material);
    }

    @Test
    void shouldTranslateTheSpringPageIntoTheDomainPageNewestFirst() {
        // Arrange
        var entity = new CurricularMaterialJpaEntity();
        var material = material();
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(jpaRepository.findAllByCourseId(eq(courseId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 1), 3));
        when(mapper.toDomain(entity)).thenReturn(material);

        // Act
        var page = repository.findAllByCourseId(courseId, Pagination.of(1, 1));

        // Assert
        verify(jpaRepository).findAllByCourseId(eq(courseId.value()), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "uploadedAt"));
        assertThat(page.items()).containsExactly(material);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void shouldFindByStatusName() {
        when(jpaRepository.findAllByStatus("PENDING_INGESTION")).thenReturn(List.of());

        assertThat(repository.findAllByStatus(IngestionStatus.PENDING_INGESTION)).isEmpty();
    }
}
