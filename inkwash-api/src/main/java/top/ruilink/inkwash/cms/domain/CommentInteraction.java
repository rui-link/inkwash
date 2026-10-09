package top.ruilink.inkwash.cms.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * A user's like on a comment.
 *
 * <p>
 * Mirrors {@link Interaction} but is scoped to comments: the article-level
 * {@code cms_interaction} table is keyed {@code UNIQUE (article_id, actor_id)}
 * and cannot also key comment likes without either a nullable, mutually
 * exclusive {@code article_id} or a sentinel value. Decision {@code ISS-034} /
 * D-06 picked a separate table for that reason.
 *
 * <p>
 * Column {@code actor_id} keeps the (misspelled) name used by
 * {@code cms_interaction} so the two tables read alike; the Java property is
 * spelled {@code actorId} and bridged by {@code @Result}, exactly as in
 * {@code cms_interaction}. See ISS-065.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Getter
@Setter
public class CommentInteraction {

	private Long id;

	private Long commentId;

	private Long actorId;

	private Boolean agree = false;
}