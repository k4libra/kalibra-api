package com.kalibra.api.enrollment.infrastructure.persistence.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations", schema = "enrollment")
public class InvitationJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String holderId;

    @Column(nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private String invitedEmail;

    @Column(nullable = false)
    private String status;

    @Embedded
    private InvitationValidityEmbeddable validity;

    private Instant respondedAt;

    public InvitationJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public InvitationValidityEmbeddable getValidity() {
        return validity;
    }

    public void setValidity(InvitationValidityEmbeddable validity) {
        this.validity = validity;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}