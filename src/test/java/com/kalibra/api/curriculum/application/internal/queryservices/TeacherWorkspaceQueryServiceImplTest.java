package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.queries.GetTeacherWorkspaceByHolderIdQuery;
import com.kalibra.api.curriculum.domain.repositories.TeacherWorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherWorkspaceQueryServiceImplTest {

    @Mock
    TeacherWorkspaceRepository teacherWorkspaceRepository;

    @InjectMocks
    TeacherWorkspaceQueryServiceImpl service;

    @Test
    void shouldReturnTheWorkspaceOfTheTeacher() {
        var workspace = TeacherWorkspace.createFor("teacher-1");
        when(teacherWorkspaceRepository.findByHolderId("teacher-1")).thenReturn(Optional.of(workspace));

        assertThat(service.handle(new GetTeacherWorkspaceByHolderIdQuery("teacher-1"))).contains(workspace);
    }

    @Test
    void shouldReturnEmptyWhenTheTeacherHasNoWorkspace() {
        when(teacherWorkspaceRepository.findByHolderId("teacher-1")).thenReturn(Optional.empty());

        assertThat(service.handle(new GetTeacherWorkspaceByHolderIdQuery("teacher-1"))).isEmpty();
    }
}
