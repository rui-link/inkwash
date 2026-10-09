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

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import top.ruilink.inkwash.cms.api.query.CommentQuery;
import top.ruilink.inkwash.cms.domain.Comment;

/**
 * Comment persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface CommentMapper {

	/**
	 * Status of a comment that is visible.
	 */
	int STATUS_VISIBLE = 1;

	@Select("SELECT * FROM cms_comment WHERE id = #{commentId}")
	@Results(id = "commentResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "articleId", column = "article_id"),
			@Result(property = "commenterId", column = "commenter_id"),
			@Result(property = "parentId", column = "parent_id"), @Result(property = "content", column = "content"),
			@Result(property = "tally.agreeCount", column = "agree_count"),
			@Result(property = "createTime", column = "create_time") })
	Comment selectById(@Param("commentId") Long commentId);

	@Select("SELECT * FROM cms_comment WHERE article_id = #{articleId} AND status = " + CommentMapper.STATUS_VISIBLE
			+ " " + "ORDER BY create_time ASC")
	@ResultMap("commentResult")
	List<Comment> selectByArticleId(@Param("articleId") Long articleId);

	@Insert("INSERT INTO cms_comment (article_id, commenter_id, parent_id, content, agree_count, status, create_time) "
			+ "VALUES (#{articleId}, #{commenterId}, #{parentId}, #{content}, " + "COALESCE(#{tally.agreeCount}, 0), "
			+ CommentMapper.STATUS_VISIBLE + ", #{createTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long insert(Comment comment);

	@Delete("DELETE FROM cms_comment WHERE id = #{commentId}")
	int deleteById(@Param("commentId") Long commentId);

	/**
	 * Deletes every comment row belonging to an article, including replies.
	 *
	 * <p>
	 * Replies are stored flat with a {@code parent_id}, so a single
	 * {@code WHERE article_id = ?} covers the whole thread — no recursion needed.
	 * Callers must delete {@code cms_comment_interaction} rows first, because those
	 * reference comments via {@code comment_id}.
	 */
	@Delete("DELETE FROM cms_comment WHERE article_id = #{articleId}")
	int deleteByArticleId(@Param("articleId") Long articleId);

	@Update("UPDATE cms_comment SET agree_count = agree_count + 1 WHERE id = #{commentId}")
	int incrementAgreeCount(@Param("commentId") Long commentId);

	@Update("UPDATE cms_comment SET agree_count = GREATEST(0, agree_count - 1) WHERE id = #{commentId}")
	int decrementAgreeCount(@Param("commentId") Long commentId);

	@Select("<script>SELECT * FROM cms_comment " + "<where>"
			+ "  <if test='query != null and query.content != null and query.content != \"\"'> AND content LIKE CONCAT('%', #{query.content}, '%') </if>"
			+ "  <if test='query != null and query.articleId != null'> AND article_id = #{query.articleId} </if>"
			+ "</where>" + "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}</script>")
	@ResultMap("commentResult")
	List<Comment> selectPage(@Param("query") CommentQuery query, @Param("offset") int offset,
			@Param("limit") int limit);

	@Select("<script>SELECT COUNT(*) FROM cms_comment " + "<where>"
			+ "  <if test='query != null and query.content != null and query.content != \"\"'> AND content LIKE CONCAT('%', #{query.content}, '%') </if>"
			+ "  <if test='query != null and query.articleId != null'> AND article_id = #{query.articleId} </if>"
			+ "</where></script>")
	long count(@Param("query") CommentQuery query);

	@Select("SELECT COUNT(*) FROM cms_comment WHERE article_id = #{articleId} AND status = "
			+ CommentMapper.STATUS_VISIBLE)
	int countByArticleId(@Param("articleId") Long articleId);

	@Select("SELECT article_id FROM cms_comment WHERE commenter_id = #{userId} GROUP BY article_id ORDER BY MAX(create_time) DESC LIMIT #{limit} OFFSET #{offset}")
	List<Long> selectDistinctArticleIdsByCommenterPaged(@Param("userId") Long userId, @Param("limit") int limit,
			@Param("offset") int offset);

	@Select("SELECT COUNT(*) FROM cms_comment WHERE commenter_id = #{userId}")
	long countByCommenter(@Param("userId") Long userId);

	@Select("SELECT COUNT(DISTINCT article_id) FROM cms_comment WHERE commenter_id = #{userId}")
	long countDistinctArticlesByCommenter(@Param("userId") Long userId);
}
