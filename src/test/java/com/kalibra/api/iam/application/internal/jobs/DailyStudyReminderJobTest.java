package com.kalibra.api.iam.application.internal.jobs;

import com.kalibra.api.iam.application.internal.outboundservices.notifications.ReminderNotificationService;
import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesWithReminderDueQuery;
import com.kalibra.api.iam.domain.services.StudentPreferencesQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyStudyReminderJobTest {

    @Mock
    StudentPreferencesQueryService studentPreferencesQueryService;

    @Mock
    ReminderNotificationService reminderNotificationService;

    @InjectMocks
    DailyStudyReminderJob job;

    @Test
    void shouldNotifyEveryHolderWhoseReminderIsDue() {
        // Arrange
        var first = StudentPreferences.createDefaultFor("holder-1");
        var second = StudentPreferences.createDefaultFor("holder-2");
        when(studentPreferencesQueryService.handle(any(GetStudentPreferencesWithReminderDueQuery.class)))
                .thenReturn(List.of(first, second));

        // Act
        job.run();

        // Assert
        verify(reminderNotificationService).notifyDailyReminder("holder-1");
        verify(reminderNotificationService).notifyDailyReminder("holder-2");
    }

    @Test
    void shouldNotNotifyAnyoneWhenNoReminderIsDue() {
        // Arrange
        when(studentPreferencesQueryService.handle(any(GetStudentPreferencesWithReminderDueQuery.class)))
                .thenReturn(List.of());

        // Act
        job.run();

        // Assert
        verify(reminderNotificationService, never()).notifyDailyReminder(any());
    }

    @Test
    void shouldKeepNotifyingRemainingHoldersWhenOneNotificationFails() {
        // Arrange
        var first = StudentPreferences.createDefaultFor("holder-1");
        var second = StudentPreferences.createDefaultFor("holder-2");
        when(studentPreferencesQueryService.handle(any(GetStudentPreferencesWithReminderDueQuery.class)))
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("push provider down"))
                .when(reminderNotificationService).notifyDailyReminder("holder-1");

        // Act
        job.run();

        // Assert
        verify(reminderNotificationService).notifyDailyReminder("holder-2");
    }
}
