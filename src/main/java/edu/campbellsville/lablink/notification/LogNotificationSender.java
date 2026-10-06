package edu.campbellsville.lablink.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development implementation: writes notifications to the console instead of
 * sending email, so nobody needs SMTP settings to run the app locally.
 *
 * When we're ready for real email, add an EmailNotificationSender that
 * implements NotificationSender and use it in production. No class that sends
 * notifications has to change.
 */
@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("[notification] to={} subject=\"{}\" body=\"{}\"", toEmail, subject, body);
    }
}
