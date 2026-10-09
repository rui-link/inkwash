/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.cms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.*;

import top.ruilink.inkwash.cms.domain.Interaction;

/**
 * MyBatis mapper for article interactions such as likes and bookmarks.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface InteractionMapper {

	/**
	 * Queries a user's interaction records.
	 */
	@Select("SELECT * FROM cms_interaction WHERE article_id = #{articleId} AND actor_id = #{actorId}")
	@Results({ @Result(property = "id", column = "id"), @Result(property = "articleId", column = "article_id"),
			@Result(property = "actorId", column = "actor_id"), @Result(property = "agree", column = "agree"),
			@Result(property = "favorite", column = "favorite"), @Result(property = "share", column = "share"),
			@Result(property = "averse", column = "averse"), @Result(property = "createTime", column = "create_time") })
	Interaction selectByArticleAndUser(@Param("articleId") Long articleId, @Param("actorId") Long actorId);

	/**
	 * Inserts an interaction record.
	 *
	 * <p>
	 * {@code create_time} is written on insert only. It records the first *
	 * interaction between the pair, so toggling a flag later must not * refresh it
	 * (see {@link Interaction#getCreateTime()}).
	 */
	@Insert("INSERT INTO cms_interaction (article_id, actor_id, agree, favorite, share, averse, create_time) "
			+ "VALUES (#{articleId}, #{actorId}, #{agree}, #{favorite}, #{share}, #{averse}, #{createTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(Interaction interaction);

	/**
	 * Updates an interaction record.
	 */
	@Update("UPDATE cms_interaction SET agree = #{agree}, favorite = #{favorite}, share = #{share}, averse = #{averse} "
			+ "WHERE article_id = #{articleId} AND actor_id = #{actorId}")
	int update(Interaction interaction);

	/**
	 * Deletes an interaction record.
	 */
	@Delete("DELETE FROM cms_interaction WHERE article_id = #{articleId} AND actor_id = #{actorId}")
	int delete(@Param("articleId") Long articleId, @Param("actorId") Long actorId);

	/**
	 * Deletes every interaction row belonging to an article.
	 *
	 * <p>
	 * Called from {@code ArticleServiceImpl.deleteArticle} inside the same
	 * transaction. Without it {@code cms_interaction} keeps one orphan row per
	 * (article, user) pair for everyone who ever interacted, and the
	 * {@code idx_actor_*_time} indexes grow without bound.
	 */
	@Delete("DELETE FROM cms_interaction WHERE article_id = #{articleId}")
	int deleteByArticleId(@Param("articleId") Long articleId);

	/**
	 * Ordering note shared by the three paged interaction queries.
	 *
	 * <p>
	 * They order by {@code create_time DESC, id DESC}. Before D-16 they ordered by
	 * {@code id DESC} only, which matched the javadoc by accident because the
	 * unique key {@code (article_id, actor_id)} means one row per pair: the
	 * surrogate key increases in step with the first interaction, so
	 * {@code id DESC} was a proxy for time. The proxy broke in one visible way —
	 * un-favouriting then re-favouriting an article left the row (and therefore its
	 * position) untouched, so the article never returned to the top.
	 *
	 * <p>
	 * {@code id DESC} is kept as a tiebreaker: {@code create_time} has one-second
	 * resolution on MySQL {@code DATETIME}, so same-second interactions would
	 * otherwise order non-deterministically across pages.
	 */

	/**
	 * Queries the IDs of articles a user bookmarked, paginated by interaction time
	 * descending.
	 */
	@Select("SELECT article_id FROM cms_interaction WHERE actor_id = #{userId} AND favorite = true ORDER BY create_time DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
	List<Long> selectFavoriteArticleIdsPaged(@Param("userId") Long userId, @Param("limit") int limit,
			@Param("offset") int offset);

	/**
	 * Counts a user's bookmarks.
	 */
	@Select("SELECT COUNT(*) FROM cms_interaction WHERE actor_id = #{userId} AND favorite = true")
	long countFavorites(@Param("userId") Long userId);

	/**
	 * Queries the IDs of articles a user down-voted, paginated by interaction time
	 * descending.
	 */
	@Select("SELECT article_id FROM cms_interaction WHERE actor_id = #{userId} AND averse = true ORDER BY create_time DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
	List<Long> selectAverseArticleIdsPaged(@Param("userId") Long userId, @Param("limit") int limit,
			@Param("offset") int offset);

	/**
	 * Counts a user's down-votes.
	 */
	@Select("SELECT COUNT(*) FROM cms_interaction WHERE actor_id = #{userId} AND averse = true")
	long countAverses(@Param("userId") Long userId);

	/**
	 * Queries the IDs of articles a user liked, paginated by interaction time
	 * descending.
	 */
	@Select("SELECT article_id FROM cms_interaction WHERE actor_id = #{userId} AND agree = true ORDER BY create_time DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
	List<Long> selectAgreeArticleIdsPaged(@Param("userId") Long userId, @Param("limit") int limit,
			@Param("offset") int offset);

	/**
	 * Counts a user's likes.
	 */
	@Select("SELECT COUNT(*) FROM cms_interaction WHERE actor_id = #{userId} AND agree = true")
	long countAgrees(@Param("userId") Long userId);

	@Select("SELECT COUNT(*) FROM cms_interaction WHERE article_id IN (SELECT id FROM cms_article WHERE author_id = #{authorId}) AND agree = TRUE")
	long countAgreesReceived(@Param("authorId") Long authorId);

	@Select("SELECT COUNT(*) FROM cms_interaction WHERE article_id IN (SELECT id FROM cms_article WHERE author_id = #{authorId}) AND favorite = TRUE")
	long countFavoritesReceived(@Param("authorId") Long authorId);

	@Select("SELECT COUNT(*) FROM cms_interaction WHERE actor_id = #{userId} AND agree = TRUE")
	long countAgreesByUser(@Param("userId") Long userId);

	@Select("SELECT COUNT(*) FROM cms_interaction WHERE actor_id = #{userId} AND favorite = TRUE")
	long countFavoritesByUser(@Param("userId") Long userId);
}
