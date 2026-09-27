package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.events.CourseCreated;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.services.CourseCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CourseCommandServiceImpl implements CourseCommandService {

    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CourseCommandServiceImpl(CourseRepository courseRepository, ApplicationEventPublisher eventPublisher) {
        this.courseRepository = courseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Course handle(CreateCourseCommand command) {
        var course = Course.create(command);
        var saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseCreated(saved.getId().value(), saved.getHolderId()));
        return saved;
    }
}
