package com.kalibra.api.curriculum.infrastructure.persistence.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.SubtopicEmbeddable;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseJpaMapperTest {

    private final CourseJpaMapper mapper = Mappers.getMapper(CourseJpaMapper.class);

    @Test
    void shouldPersistAndRestoreActiveAndArchivedSubtopicsWithOrderAndIds() {
        var course = Course.create(new CreateCourseCommand("teacher", "Algebra", new CourseCode("MAT"), List.of("Equations", "Functions")));
        course.getSubtopics().getFirst().setStatus(SubtopicStatus.ARCHIVED);
        var entity = mapper.toEntity(course);
        assertThat(entity.getSubtopics()).extracting(SubtopicEmbeddable::getStatus).containsExactly(SubtopicStatus.ARCHIVED, SubtopicStatus.ACTIVE);
        var restored = mapper.toDomain(entity);
        assertThat(restored).usingRecursiveComparison().isEqualTo(course);
    }

    @Test
    void shouldMapStatusAsRequiredVarcharCompatibleWithV3AndExistingActiveRows() throws Exception {
        var status = SubtopicEmbeddable.class.getDeclaredField("status");
        assertThat(status.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(status.getAnnotation(Column.class).nullable()).isFalse();
        assertThat(status.getAnnotation(Column.class).length()).isEqualTo(20);
        assertThat(new SubtopicEmbeddable().getStatus()).isEqualTo(SubtopicStatus.ACTIVE);
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/curriculum/V3__add_subtopic_status.sql")))
                .contains("ALTER TABLE curriculum.course_subtopics", "ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
    }
}
