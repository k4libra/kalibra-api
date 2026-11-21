package com.kalibra.api.iam.application.internal.jobs;

import com.kalibra.api.iam.application.internal.outboundservices.notifications.ReminderNotificationService;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesWithReminderDueQuery;
import com.kalibra.api.iam.domain.services.StudentPreferencesQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class DailyStudyReminderJob {

    private static final Logger log = LoggerFactory.getLogger(DailyStudyReminderJob.class);

    private final StudentPreferencesQueryService studentPreferencesQueryService;
    private final ReminderNotificationService reminderNotificationService;

    public DailyStudyReminderJob(StudentPreferencesQueryService studentPreferencesQueryService,
                                  ReminderNotificationService reminderNotificationService) {
        this.studentPreferencesQueryService = studentPreferencesQueryService;
        this.reminderNotificationService = reminderNotificationService;
    }

    @Scheduled(cron = "0 * * * * *", zone = "UTC")
    public void run() {
        var now = LocalTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);
        var dueQuery = new GetStudentPreferencesWithReminderDueQuery(now);
        for (var preferences : studentPreferencesQueryService.handle(dueQuery)) {
            try {
                reminderNotificationService.notifyDailyReminder(preferences.getHolderId());
            } catch (RuntimeException failure) {
                log.error("Daily study reminder failed for holder {}", preferences.getHolderId(), failure);
            }
        }
    }
}
