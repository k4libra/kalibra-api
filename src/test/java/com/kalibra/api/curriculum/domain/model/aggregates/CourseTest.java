package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseTest {

    private static CreateCourseCommand command(List<String> subtopicNames) {
        return new CreateCourseCommand("teacher-1", " Algebra I ", new CourseCode("MAT101"), subtopicNames);
    }

    @Test
    void shouldCreateCourseWithSubtopicsInDisplayOrder() {
        // Act
        var course = Course.create(command(List.of("Linear equations", "Quadratic equations")));

        // Assert
        assertThat(course.getId()).isNotNull();
        assertThat(course.getHolderId()).isEqualTo("teacher-1");
        assertThat(course.getName()).isEqualTo("Algebra I");
        assertThat(course.getCode().value()).isEqualTo("MAT101");
        assertThat(course.getCreatedAt()).isNotNull();
        assertThat(course.getSubtopics()).extracting("name").containsExactly("Linear equations", "Quadratic equations");
        assertThat(course.getSubtopics()).extracting("displayOrder").containsExactly(1, 2);
        assertThat(course.getSubtopics()).allSatisfy(subtopic -> assertThat(subtopic.getId()).isNotNull());
    }

    @Test
    void shouldRejectCourseWithoutSubtopics() {
        assertThatThrownBy(() -> Course.create(command(List.of())))
                .isInstanceOf(CourseWithoutSubtopicsException.class);
        assertThatThrownBy(() -> Course.create(command(null)))
                .isInstanceOf(CourseWithoutSubtopicsException.class);
    }

    @Test
    void shouldKnowItsSubtopicsAndOwner() {
        // Arrange
        var course = Course.create(command(List.of("Linear equations")));
        var ownSubtopic = course.getSubtopics().getFirst().getId();

        // Act & Assert
        assertThat(course.hasSubtopic(ownSubtopic)).isTrue();
        assertThat(course.hasSubtopic(new SubtopicId(UUID.randomUUID()))).isFalse();
        assertThat(course.isOwnedBy("teacher-1")).isTrue();
        assertThat(course.isOwnedBy("teacher-2")).isFalse();
    }
}
