package edu.campbellsville.lablink.notification;

/**
 * DEPENDENCY INVERSION: the abstraction the rest of the app depends on.
 *
 * Code that needs to notify someone asks for a NotificationSender, never for
 * a specific email library. Which implementation runs (log output in
 * development, real email in production, a fake in tests) is chosen by
 * Spring configuration, not by the code that sends the message.
 */
public interface NotificationSender {

    void send(String toEmail, String subject, String body);
}
