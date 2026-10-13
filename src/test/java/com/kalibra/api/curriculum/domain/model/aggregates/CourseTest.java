package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.exceptions.LastActiveSubtopicException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRemovalException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRenameException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicNotFoundException;
import com.kalibra.api.curriculum.domain.model.commands.AddSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.RemoveSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
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

    private Course editableCourse() {
        return Course.create(new CreateCourseCommand("teacher", "Algebra", new CourseCode("MAT101"),
                List.of("Equations", "Inequalities", "Functions")));
    }

    private UpdateSubtopicCommand update(Course course, SubtopicId id, String name, Integer order, SubtopicStatus status) {
        return new UpdateSubtopicCommand(course.getId(), "teacher", id, Optional.ofNullable(name),
                Optional.ofNullable(order), Optional.ofNullable(status));
    }

    @Test
    void shouldUpdateOnlySuppliedCourseFields() {
        var course = editableCourse();
        course.update(new UpdateCourseCommand(course.getId(), "teacher", Optional.of(" Geometry "), Optional.empty()));
        assertThat(course.getName()).isEqualTo("Geometry");
        assertThat(course.getCode()).isEqualTo(new CourseCode("MAT101"));
        course.update(new UpdateCourseCommand(course.getId(), "teacher", Optional.empty(), Optional.of(new CourseCode("GEO"))));
        assertThat(course.getCode()).isEqualTo(new CourseCode("GEO"));
        assertThat(course.getName()).isEqualTo("Geometry");
    }

    @Test
    void shouldAppendActiveSubtopicAfterArchivedOnes() {
        var course = editableCourse();
        course.updateSubtopic(update(course, course.getSubtopics().getLast().getId(), null, null, SubtopicStatus.ARCHIVED), false);
        course.addSubtopic(new AddSubtopicCommand(course.getId(), "teacher", " Limits "));
        var last = course.getSubtopics().getLast();
        assertThat(last.getName()).isEqualTo("Limits");
        assertThat(last.isActive()).isTrue();
        assertThat(last.getDisplayOrder()).isEqualTo(4);
        assertThat(course.getSubtopics()).extracting(Subtopic::getId).doesNotHaveDuplicates();
    }

    @Test
    void shouldRenameUnlockedSubtopicAndPreserveOmittedFields() {
        var course = editableCourse();
        var first = course.getSubtopics().getFirst();
        course.updateSubtopic(update(course, first.getId(), " Linear equations ", null, null), false);
        assertThat(first.getName()).isEqualTo("Linear equations");
        assertThat(first.getDisplayOrder()).isEqualTo(1);
        assertThat(first.isActive()).isTrue();
    }

    @Test
    void shouldRejectLockedRenameBeforeApplyingOtherFields() {
        var course = editableCourse();
        var first = course.getSubtopics().getFirst();
        assertThatThrownBy(() -> course.updateSubtopic(update(course, first.getId(), "Renamed", 3, SubtopicStatus.ARCHIVED), true))
                .isInstanceOf(SubtopicLockedForRenameException.class);
        assertThat(first.getName()).isEqualTo("Equations");
        assertThat(first.isActive()).isTrue();
        assertThat(first.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void shouldArchiveRestoreAndReorderLockedSubtopic() {
        var course = editableCourse();
        var first = course.getSubtopics().getFirst();
        course.updateSubtopic(update(course, first.getId(), null, 3, SubtopicStatus.ARCHIVED), true);
        assertThat(first.isActive()).isFalse();
        assertThat(course.hasActiveSubtopic(first.getId())).isFalse();
        course.updateSubtopic(update(course, first.getId(), null, 1, SubtopicStatus.ACTIVE), true);
        assertThat(first.isActive()).isTrue();
        assertThat(course.getSubtopics()).extracting(Subtopic::getName).containsExactly("Equations", "Inequalities", "Functions");
        assertThat(course.getSubtopics()).extracting(Subtopic::getDisplayOrder).containsExactly(1, 2, 3);
    }

    @Test
    void shouldShiftOthersWhenMovingInEitherDirection() {
        var course = editableCourse();
        var first = course.getSubtopics().getFirst();
        course.updateSubtopic(update(course, first.getId(), null, 3, null), false);
        assertThat(course.getSubtopics()).extracting(Subtopic::getName).containsExactly("Inequalities", "Functions", "Equations");
        assertThat(course.getSubtopics()).extracting(Subtopic::getDisplayOrder).containsExactly(1, 2, 3);
        course.updateSubtopic(update(course, first.getId(), null, 2, null), false);
        assertThat(course.getSubtopics()).extracting(Subtopic::getName).containsExactly("Inequalities", "Equations", "Functions");
        assertThat(course.getSubtopics()).extracting(Subtopic::getDisplayOrder).containsExactly(1, 2, 3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4})
    void shouldRejectOutOfRangeOrderWithoutRenaming(int order) {
        var course = editableCourse();
        var first = course.getSubtopics().getFirst();
        assertThatThrownBy(() -> course.updateSubtopic(update(course, first.getId(), "Renamed", order, null), false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(first.getName()).isEqualTo("Equations");
    }

    @Test
    void shouldKeepLastActiveSubtopicWhenOthersAreArchived() {
        var course = editableCourse();
        for (var subtopic : course.getSubtopics().subList(1, 3)) {
            course.updateSubtopic(update(course, subtopic.getId(), null, null, SubtopicStatus.ARCHIVED), false);
        }
        var first = course.getSubtopics().getFirst();
        assertThatThrownBy(() -> course.updateSubtopic(update(course, first.getId(), null, null, SubtopicStatus.ARCHIVED), false))
                .isInstanceOf(LastActiveSubtopicException.class);
        assertThatThrownBy(() -> course.removeSubtopic(new RemoveSubtopicCommand(course.getId(), "teacher", first.getId()), false))
                .isInstanceOf(LastActiveSubtopicException.class);
        assertThat(first.isActive()).isTrue();
    }

    @Test
    void shouldRemoveArchivedSubtopicAndCloseOrderGap() {
        var course = editableCourse();
        var middle = course.getSubtopics().get(1);
        course.updateSubtopic(update(course, middle.getId(), null, null, SubtopicStatus.ARCHIVED), false);
        course.removeSubtopic(new RemoveSubtopicCommand(course.getId(), "teacher", middle.getId()), false);
        assertThat(course.getSubtopics()).extracting(Subtopic::getDisplayOrder).containsExactly(1, 2);
        assertThat(course.getSubtopics()).extracting(Subtopic::getName).containsExactly("Equations", "Functions");
    }

    @Test
    void shouldRemoveActiveSubtopicWhenAnotherRemains() {
        var course = editableCourse();
        course.removeSubtopic(new RemoveSubtopicCommand(course.getId(), "teacher", course.getSubtopics().getFirst().getId()), false);
        assertThat(course.getSubtopics()).extracting(Subtopic::getDisplayOrder).containsExactly(1, 2);
        assertThat(course.getSubtopics()).allMatch(Subtopic::isActive);
    }

    @Test
    void shouldRejectLockedRemovalEvenWhenArchived() {
        var course = editableCourse();
        var first = course.getSubtopics().getFirst();
        course.updateSubtopic(update(course, first.getId(), null, null, SubtopicStatus.ARCHIVED), true);
        assertThatThrownBy(() -> course.removeSubtopic(new RemoveSubtopicCommand(course.getId(), "teacher", first.getId()), true))
                .isInstanceOf(SubtopicLockedForRemovalException.class);
        assertThat(course.getSubtopics()).hasSize(3);
    }

    @Test
    void shouldRejectUnknownSubtopicOnUpdateAndRemoval() {
        var course = editableCourse();
        var unknown = new SubtopicId(UUID.randomUUID());
        assertThatThrownBy(() -> course.updateSubtopic(update(course, unknown, "Name", null, null), false))
                .isInstanceOf(SubtopicNotFoundException.class);
        assertThatThrownBy(() -> course.removeSubtopic(new RemoveSubtopicCommand(course.getId(), "teacher", unknown), false))
                .isInstanceOf(SubtopicNotFoundException.class);
    }
}
