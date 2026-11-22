package edu.campbellsville.lablink.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Writes one log line for every login attempt. A burst of auth.login.failure lines for one email
 * is the signal for a password-guessing attack (see docs/monitoring.md). Passwords are never logged.
 */
@Component
public class SecurityEventLogger {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityEventLogger.class);

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        LOG.info("auth.login.success user={}", event.getAuthentication().getName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        LOG.warn("auth.login.failure user={} reason={}", event.getAuthentication().getName(),
                event.getException().getClass().getSimpleName());
    }
}
