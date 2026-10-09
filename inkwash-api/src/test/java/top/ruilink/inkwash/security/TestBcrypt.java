package top.ruilink.inkwash.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt password encoder round-trip scratch utility.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class TestBcrypt {
	public static void main(String[] args) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String hash = encoder.encode("welcome");
		System.out.println("Hash: " + hash);
		System.out.println("Matches: " + encoder.matches("welcome", hash));
		System.out.println("Test against stored: "
				+ encoder.matches("welcome", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7pZldHbMbgLGlRqHQuJrY3F6"));
	}
}