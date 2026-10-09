package top.ruilink.inkwash.monitor.domain;

/**
 * Logout event carrying the user whose session is being closed.
 *
 * <p>
 * Authentication events include logout. Without this, {@code mon_login_info}
 * only ever records session <em>starts</em> and a login session is never closed
 * in the log, so the authentication trail cannot be reconstructed.
 * {@code mon_journal} deliberately does not cover this — the two tables have
 * separate responsibilities (see the design document, §6.2.1).
 *
 * <p>
 * Only the user id is carried: closing a session needs no more, and the logout
 * timestamp is taken by the listener at write time rather than being captured
 * here.
 *
 * @param userId id of the user logging out; may be {@code null} for anonymous
 *               requests, which the listener ignores
 * @author Dyllon
 * @since 0.5.1
 */
public record LogoutEvent(Long userId) {
}