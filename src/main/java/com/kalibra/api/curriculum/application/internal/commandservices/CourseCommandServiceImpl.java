package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotFoundException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.AddSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.RemoveSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.events.CourseCreated;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.services.CourseCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseCommandServiceImpl implements CourseCommandService {

    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CurricularMaterialRepository curricularMaterialRepository;

    public CourseCommandServiceImpl(CourseRepository courseRepository, ApplicationEventPublisher eventPublisher,
                                    CurricularMaterialRepository curricularMaterialRepository) {
        this.courseRepository = courseRepository;
        this.eventPublisher = eventPublisher;
        this.curricularMaterialRepository = curricularMaterialRepository;
    }

    @Override
    public Course handle(CreateCourseCommand command) {
        var course = Course.create(command);
        var saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseCreated(saved.getId().value(), saved.getHolderId()));
        return saved;
    }

    @Override
    @Transactional
    public Course handle(UpdateCourseCommand command) {
        var course = ownedCourse(command.courseId(), command.holderId());
        course.update(command);
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course handle(AddSubtopicCommand command) {
        var course = ownedCourse(command.courseId(), command.holderId());
        course.addSubtopic(command);
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course handle(UpdateSubtopicCommand command) {
        var course = ownedCourse(command.courseId(), command.holderId());
        var locked = curricularMaterialRepository.existsByCourseIdAndSubtopicId(command.courseId(), command.subtopicId());
        course.updateSubtopic(command, locked);
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public void handle(RemoveSubtopicCommand command) {
        var course = ownedCourse(command.courseId(), command.holderId());
        var locked = curricularMaterialRepository.existsByCourseIdAndSubtopicId(command.courseId(), command.subtopicId());
        course.removeSubtopic(command, locked);
        courseRepository.save(course);
    }

    private Course ownedCourse(CourseId courseId, String holderId) {
        return courseRepository.findByIdAndHolderId(courseId, holderId)
                .orElseThrow(() -> new CourseNotFoundException(courseId.value()));
    }
}
