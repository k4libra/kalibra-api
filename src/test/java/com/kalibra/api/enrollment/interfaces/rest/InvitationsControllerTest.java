package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotPendingException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotResendableException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAccountNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyEnrolledException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyInvitedException;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.CancelInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.RejectInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.ResendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.queries.GetPendingInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.PendingInvitationView;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.services.InvitationCommandService;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.transform.InvitationAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, InvitationAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class InvitationsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InvitationCommandService commandService;

    @MockitoBean
    InvitationQueryService queryService;

    private final String studentId = UUID.randomUUID().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final Invitation invitation = Invitation.send(
            new SendInvitationCommand("teacher-1", courseId, new Email("ana@kalibra.pe")),
            new StudentId(UUID.fromString(studentId)));

    private String sendBody(String email) {
        return "{\"courseId\":\"" + courseId.value() + "\",\"studentEmail\":\"" + email + "\"}";
    }

    @Test
    void shouldSendAnInvitation() throws Exception {
        when(commandService.handle(new SendInvitationCommand("teacher-1", courseId, new Email("ana@kalibra.pe"))))
                .thenReturn(invitation);

        mockMvc.perform(post("/api/v1/invitations").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("ana@kalibra.pe")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/invitations/" + invitation.getId().value()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invitedEmail").value("ana@kalibra.pe"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void shouldReturnBadRequestWithoutCourse() throws Exception {
        mockMvc.perform(post("/api/v1/invitations").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"studentEmail\":\"ana@kalibra.pe\"}"))
                .andExpect(status().isBadRequest());
        verify(commandService, never()).handle(any(SendInvitationCommand.class));
    }

    @Test
    void shouldReturnUnprocessableForAnEmailWithoutStudentAccount() throws Exception {
        when(commandService.handle(any(SendInvitationCommand.class)))
                .thenThrow(new StudentAccountNotFoundException("nobody@kalibra.pe"));

        mockMvc.perform(post("/api/v1/invitations").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("nobody@kalibra.pe")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void shouldReturnNotFoundForACourseOfAnotherTeacher() throws Exception {
        when(commandService.handle(any(SendInvitationCommand.class))).thenThrow(new CourseNotOwnedByTeacherException());

        mockMvc.perform(post("/api/v1/invitations").with(user("teacher-2").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("ana@kalibra.pe")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictForAStudentAlreadyInvited() throws Exception {
        when(commandService.handle(any(SendInvitationCommand.class))).thenThrow(new StudentAlreadyInvitedException());

        mockMvc.perform(post("/api/v1/invitations").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("ana@kalibra.pe")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnConflictForAStudentAlreadyEnrolled() throws Exception {
        when(commandService.handle(any(SendInvitationCommand.class))).thenThrow(new StudentAlreadyEnrolledException());

        mockMvc.perform(post("/api/v1/invitations").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("ana@kalibra.pe")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListThePendingInvitationsOfTheStudent() throws Exception {
        var view = new PendingInvitationView(invitation.getId().value(), "Algebra", "teacher@kalibra.pe",
                invitation.getValidity().sentAt(), invitation.getValidity().expiresAt());
        when(queryService.handle(new GetPendingInvitationsByHolderIdQuery(studentId))).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/invitations").param("status", "pending").with(user(studentId).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invitation.getId().value().toString()))
                .andExpect(jsonPath("$[0].courseName").value("Algebra"))
                .andExpect(jsonPath("$[0].teacherEmail").value("teacher@kalibra.pe"));
    }

    @Test
    void shouldListPendingInvitationsByDefault() throws Exception {
        when(queryService.handle(new GetPendingInvitationsByHolderIdQuery(studentId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/invitations").with(user(studentId).roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestForAnUnsupportedStatus() throws Exception {
        mockMvc.perform(get("/api/v1/invitations").param("status", "ACCEPTED").with(user(studentId).roles("STUDENT")))
                .andExpect(status().isBadRequest());
        verify(queryService, never()).handle(any(GetPendingInvitationsByHolderIdQuery.class));
    }

    @Test
    void shouldCancelAnInvitation() throws Exception {
        when(commandService.handle(new CancelInvitationCommand("teacher-1", invitation.getId()))).thenReturn(invitation);

        mockMvc.perform(post("/api/v1/invitations/{id}/cancellations", invitation.getId().value())
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnNotFoundForAnInvitationOfAnotherTeacher() throws Exception {
        var invitationId = UUID.randomUUID();
        when(commandService.handle(new CancelInvitationCommand("teacher-2", new InvitationId(invitationId))))
                .thenThrow(new InvitationNotFoundException());

        mockMvc.perform(post("/api/v1/invitations/{id}/cancellations", invitationId).with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenResendingAPendingInvitation() throws Exception {
        when(commandService.handle(new ResendInvitationCommand("teacher-1", invitation.getId())))
                .thenThrow(new InvitationNotResendableException());

        mockMvc.perform(post("/api/v1/invitations/{id}/renewals", invitation.getId().value())
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAcceptAnInvitation() throws Exception {
        when(commandService.handle(new AcceptInvitationCommand(studentId, invitation.getId()))).thenReturn(invitation);

        mockMvc.perform(post("/api/v1/invitations/{id}/acceptances", invitation.getId().value())
                        .with(user(studentId).roles("STUDENT")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnConflictWhenAcceptingAnExpiredInvitation() throws Exception {
        when(commandService.handle(new AcceptInvitationCommand(studentId, invitation.getId())))
                .thenThrow(new InvitationNotPendingException());

        mockMvc.perform(post("/api/v1/invitations/{id}/acceptances", invitation.getId().value())
                        .with(user(studentId).roles("STUDENT")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectAnInvitation() throws Exception {
        when(commandService.handle(new RejectInvitationCommand(studentId, invitation.getId()))).thenReturn(invitation);

        mockMvc.perform(post("/api/v1/invitations/{id}/rejections", invitation.getId().value())
                        .with(user(studentId).roles("STUDENT")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidSendingInvitationsToStudents() throws Exception {
        mockMvc.perform(post("/api/v1/invitations").with(user(studentId).roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("ana@kalibra.pe")))
                .andExpect(status().isForbidden());
        verify(commandService, never()).handle(any(SendInvitationCommand.class));
    }

    @Test
    void shouldForbidAcceptingInvitationsToTeachers() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/{id}/acceptances", UUID.randomUUID()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
        verify(commandService, never()).handle(any(AcceptInvitationCommand.class));
    }

    @Test
    void shouldForbidListingPendingInvitationsToTeachers() throws Exception {
        mockMvc.perform(get("/api/v1/invitations").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }
}
