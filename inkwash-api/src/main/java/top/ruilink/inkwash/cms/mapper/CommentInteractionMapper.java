package top.ruilink.inkwash.cms.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import top.ruilink.inkwash.cms.domain.CommentInteraction;

/**
 * Comment like persistence mapper.
 *
 * <p>
 * Mirrors {@link InteractionMapper}.
 * {@code UNIQUE KEY uk_comment_actor (comment_id, actor_id)} is what guarantees
 * at most one interaction row per user per comment.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface CommentInteractionMapper {

	@Insert("INSERT INTO cms_comment_interaction (comment_id, actor_id, agree) "
			+ "VALUES (#{commentId}, #{actorId}, #{agree})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(CommentInteraction interaction);

	@Update("UPDATE cms_comment_interaction SET agree = #{agree} "
			+ "WHERE comment_id = #{commentId} AND actor_id = #{actorId}")
	int update(CommentInteraction interaction);

	@Select("SELECT id, comment_id, actor_id, agree FROM cms_comment_interaction "
			+ "WHERE comment_id = #{commentId} AND actor_id = #{actorId}")
	@Results(id = "commentInteractionResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "commentId", column = "comment_id"), @Result(property = "actorId", column = "actor_id"),
			@Result(property = "agree", column = "agree") })
	CommentInteraction selectByCommentAndUser(@Param("commentId") Long commentId, @Param("actorId") Long actorId);

	@Delete("DELETE FROM cms_comment_interaction WHERE comment_id = #{commentId} AND actor_id = #{actorId}")
	int delete(@Param("commentId") Long commentId, @Param("actorId") Long actorId);

	/**
	 * Drops every like row for a comment.
	 *
	 * <p>
	 * Called when the comment itself is deleted. Removing only the operator's own
	 * row would leave one orphan per user who had liked it — the same accumulation
	 * ISS-012 describes for {@code cms_interaction}.
	 */
	@Delete("DELETE FROM cms_comment_interaction WHERE comment_id = #{commentId}")
	int deleteByComment(@Param("commentId") Long commentId);

	/**
	 * Deletes every comment-interaction row for all comments of an article.
	 *
	 * <p>
	 * Must run <em>before</em> {@link CommentMapper#deleteByArticleId(Long)} — once
	 * the comment rows are gone the {@code comment_id} values can no longer be
	 * resolved. The sub-select keeps this a single statement rather than one round
	 * trip per comment.
	 */
	@Delete("DELETE FROM cms_comment_interaction WHERE comment_id IN "
			+ "(SELECT id FROM cms_comment WHERE article_id = #{articleId})")
	int deleteByArticleId(@Param("articleId") Long articleId);
}