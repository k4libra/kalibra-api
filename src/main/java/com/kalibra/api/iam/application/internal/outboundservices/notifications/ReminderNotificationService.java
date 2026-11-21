package com.kalibra.api.iam.application.internal.outboundservices.notifications;

public interface ReminderNotificationService {

    void notifyDailyReminder(String holderId);
}
