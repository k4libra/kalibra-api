package com.kalibra.api.iam.application.internal.outboundservices.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushReminderNotificationService implements ReminderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushReminderNotificationService.class);

    @Override
    public void notifyDailyReminder(String holderId) {
        log.info("Daily study reminder due for holder {}", holderId);
    }
}
