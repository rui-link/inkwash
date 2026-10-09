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

import java.time.LocalDateTime;
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

import top.ruilink.inkwash.base.domain.StatisticEntry;
import top.ruilink.inkwash.base.enums.StatisticUnit;
import top.ruilink.inkwash.cms.api.query.ArticleQuery;
import top.ruilink.inkwash.cms.domain.Article;
import top.ruilink.inkwash.cms.api.view.ArticleTermView;

/**
 * MyBatis mapper for articles.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface ArticleMapper {

	/**
	 * Columns selected by list queries, without content, which the detail query
	 * loads separately.
	 */
	String LIST_COLUMNS = "id, title, slug, summary, cover_url, content_type, author_id, category_id, status, "
			+ "reviewer_id, opinion, publish_time, view_count, comment_count, agree_count, favorite_count, "
			+ "share_count, creator, create_time, updater, update_time";

	/**
	 * The published status, mirroring
	 * {@link top.ruilink.inkwash.cms.enums.ArticleStatus#PUBLISH}.
	 */
	int STATUS_PUBLISHED = 5;

	/**
	 * Week-bucket expression for {@code create_time}, selected per database vendor.
	 *
	 * <p>
	 * Only {@link #countActiveAuthorsByRange} uses this; the other statistics
	 * queries satisfy a {@link StatisticUnit#WEEK} request with plain
	 * {@code DATE(create_time)} rows. That difference is invisible in the response:
	 * the service folds rows into the same 7-day buckets either way, so a WEEK
	 * request yields weekly points in every case. Do not "unify" the two without
	 * reading commit {@code 3e3999b}.
	 *
	 * <p>
	 * Both branches snap a row's date back to the bucket that is 7 days before it,
	 * anchored on the {@code start} bind parameter — <em>not</em> on Monday. For a
	 * {@code THIS_MONTH} request beginning 2026-09-01 the boundaries are 09-01,
	 * 09-08, 09-15, 09-22 and 09-29. This is deliberate: the buckets are
	 * month-anchored, so they are not calendar weeks, which is why the chart does
	 * not label them as such. The H2 branch reverses the {@code DATEDIFF} arguments
	 * on purpose: H2's {@code DATEDIFF('DAY', a, b)} returns {@code b - a}, the
	 * opposite of MySQL's {@code DATEDIFF(a, b)}, so keeping MySQL's argument order
	 * would bucket rows one week into the future. Verified to produce
	 * byte-identical buckets on both engines.
	 *
	 * <p>
	 * {@code <otherwise>} is MySQL so an unrecognised vendor (or a missing
	 * {@code DatabaseIdProvider}) keeps today's production behaviour.
	 *
	 * <p>
	 * Only valid in the SELECT list: the matching GROUP BY must reference the
	 * {@code stat_day} alias instead of repeating this expression, because H2
	 * refuses to match a select expression against a GROUP BY expression that
	 * embeds a bind parameter
	 * ({@code Column "CREATE_TIME" must be in the GROUP BY list}).
	 */
	String WEEK_BUCKET_CREATE = "<choose>"
			+ "<when test=\"_databaseId == 'h2'\">DATEADD('DAY', -MOD(DATEDIFF('DAY', #{start}, DATE(create_time)), 7), DATE(create_time))</when>"
			+ "<otherwise>DATE_SUB(DATE(create_time), INTERVAL MOD(DATEDIFF(DATE(create_time), #{start}), 7) DAY)</otherwise>"
			+ "</choose>";

	@Select("SELECT * FROM cms_article WHERE id = #{articleId}")
	@Results(id = "articleResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "title", column = "title"), @Result(property = "slug", column = "slug"),
			@Result(property = "summary", column = "summary"), @Result(property = "coverUrl", column = "cover_url"),
			@Result(property = "content", column = "content"),
			@Result(property = "contentType", column = "content_type"),
			@Result(property = "authorId", column = "author_id"),
			@Result(property = "categoryId", column = "category_id"), @Result(property = "status", column = "status"),
			@Result(property = "reviewerId", column = "reviewer_id"), @Result(property = "opinion", column = "opinion"),
			@Result(property = "publishTime", column = "publish_time"),
			@Result(property = "viewCount", column = "view_count"),
			@Result(property = "commentCount", column = "comment_count"),
			@Result(property = "agreeCount", column = "agree_count"),
			@Result(property = "favoriteCount", column = "favorite_count"),
			@Result(property = "shareCount", column = "share_count"),
			@Result(property = "averseCount", column = "averse_count"),
			@Result(property = "creator", column = "creator"), @Result(property = "updater", column = "updater"),
			@Result(property = "createTime", column = "create_time"),
			@Result(property = "updateTime", column = "update_time") })
	Article selectById(@Param("articleId") Long articleId);

	@Select("<script>" + "SELECT * FROM cms_article WHERE id IN "
			+ "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" + "  #{id}" + "</foreach>"
			+ "</script>")
	@ResultMap("articleResult")
	List<Article> selectByIds(@Param("ids") List<Long> ids);

	@Select("SELECT " + ArticleMapper.LIST_COLUMNS
			+ " FROM cms_article ORDER BY update_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("articleResult")
	List<Article> selectAll(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT " + ArticleMapper.LIST_COLUMNS
			+ " FROM cms_article WHERE author_id = #{authorId} ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("articleResult")
	List<Article> selectByAuthorId(@Param("authorId") Long authorId, @Param("offset") int offset,
			@Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM cms_article")
	long count();

	@Select("SELECT COUNT(*) FROM cms_article WHERE author_id = #{authorId}")
	long countByAuthorId(@Param("authorId") Long authorId);

	@Select("SELECT " + ArticleMapper.LIST_COLUMNS + " FROM cms_article WHERE status = "
			+ ArticleMapper.STATUS_PUBLISHED + " ORDER BY publish_time DESC LIMIT #{limit} OFFSET #{offset}")
	@ResultMap("articleResult")
	List<Article> selectPublished(@Param("offset") int offset, @Param("limit") int limit);

	@Select("SELECT COUNT(*) FROM cms_article WHERE status = " + ArticleMapper.STATUS_PUBLISHED)
	long countPublished();

	@Insert("INSERT INTO cms_article (title, slug, summary, cover_url, content, content_type, author_id, category_id, status, reviewer_id, opinion, view_count, comment_count, agree_count, favorite_count, share_count, averse_count, creator, create_time, update_time) "
			+ "VALUES (#{title}, #{slug}, #{summary}, #{coverUrl}, #{content}, #{contentType}, #{authorId}, #{categoryId}, #{status}, #{reviewerId}, #{opinion}, #{tally.viewCount}, #{tally.commentCount}, #{tally.agreeCount}, #{tally.favoriteCount}, #{tally.shareCount}, #{tally.averseCount}, #{creator}, #{createTime}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long insert(Article article);

	/**
	 * 更新文章的内容字段。**不写status，也不写任何计数字段**：
	 * <ul>
	 * <li>状态迁移统一走 {@link #updateStatus}，避免并发迁移被内容编辑覆盖；</li>
	 * <li>计数字段只能由 {@code increment*Count} / {@code decrement*Count} 这类 原子 SQL
	 * 修改，否则会把未初始化的 tally 写成 NULL。</li>
	 * </ul>
	 */
	@Update("UPDATE cms_article SET title = #{title}, slug = #{slug}, summary = #{summary}, cover_url = #{coverUrl}, content = #{content}, content_type = #{contentType}, author_id = #{authorId}, category_id = #{categoryId}, "
			+ "updater = #{updater}, update_time = #{updateTime} WHERE id = #{id}")
	int update(Article article);

	/**
	 * 状态迁移专用更新：以「期望的旧状态」作为更新条件，实现乐观并发控制。 迁移方法先在内存中完成状态变更，再调用本方法；若返回 0，说明期间
	 * 已有其他迁移改变了状态，本次迁移必须放弃。
	 *
	 * @param expectedStatus 迁移前的状态 code
	 * @return 受影响行数，1 表示成功，0 表示状态已被并发改变
	 */
	@Update("UPDATE cms_article SET status = #{article.status}, reviewer_id = #{article.reviewerId}, opinion = #{article.opinion}, publish_time = #{article.publishTime}, "
			+ "updater = #{article.updater}, update_time = #{article.updateTime} WHERE id = #{article.id} AND status = #{expectedStatus}")
	int updateStatus(@Param("article") Article article, @Param("expectedStatus") int expectedStatus);

	@Delete("DELETE FROM cms_article WHERE id = #{articleId}")
	int deleteById(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET view_count = view_count + 1 WHERE id = #{articleId}")
	int incrementViewCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET comment_count = comment_count + 1 WHERE id = #{articleId}")
	int incrementCommentCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET comment_count = GREATEST(0, comment_count - 1) WHERE id = #{articleId}")
	int decrementCommentCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET agree_count = agree_count + 1 WHERE id = #{articleId}")
	int incrementAgreeCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET agree_count = GREATEST(0, agree_count - 1) WHERE id = #{articleId}")
	int decrementAgreeCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET favorite_count = favorite_count + 1 WHERE id = #{articleId}")
	int incrementFavoriteCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET favorite_count = GREATEST(0, favorite_count - 1) WHERE id = #{articleId}")
	int decrementFavoriteCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET share_count = share_count + 1 WHERE id = #{articleId}")
	int incrementShareCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET share_count = GREATEST(0, share_count - 1) WHERE id = #{articleId}")
	int decrementShareCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET averse_count = averse_count + 1 WHERE id = #{articleId}")
	int incrementAverseCount(@Param("articleId") Long articleId);

	@Update("UPDATE cms_article SET averse_count = GREATEST(0, averse_count - 1) WHERE id = #{articleId}")
	int decrementAverseCount(@Param("articleId") Long articleId);

	@Select("<script>" + "SELECT COUNT(1) FROM cms_article " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.title != null and query.title != \"\"'> AND title LIKE CONCAT('%', #{query.title}, '%') </if>"
			+ "  <if test='query.authorId != null'> AND author_id = #{query.authorId} </if>" + "</where>" + "</script>")
	long countArticles(@Param("query") ArticleQuery query);

	@ResultMap("articleResult")
	@Select("<script>" + "SELECT " + ArticleMapper.LIST_COLUMNS + " FROM cms_article " + "<where>"
			+ "  <if test='query.status != null'> AND status = #{query.status} </if>"
			+ "  <if test='query.title != null and query.title != \"\"'> AND title LIKE CONCAT('%', #{query.title}, '%') </if>"
			+ "  <if test='query.authorId != null'> AND author_id = #{query.authorId} </if>" + "</where>"
			+ "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}" + "</script>")
	List<Article> selectArticleList(@Param("query") ArticleQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);

	@Select("SELECT COUNT(*) FROM cms_article WHERE status = #{status}")
	long countByStatus(@Param("status") int status);

	@Select("SELECT COUNT(*) FROM cms_article WHERE category_id = #{categoryId}")
	long countByCategoryId(@Param("categoryId") Integer categoryId);

	@Select("SELECT COUNT(*) FROM cms_article_term WHERE term_id = #{termId}")
	long countByTermId(@Param("termId") Integer termId);

	@Select("SELECT id, title, status, create_time FROM cms_article WHERE status = #{status} ORDER BY create_time DESC LIMIT #{limit}")
	List<Article> selectByStatusLimit(@Param("status") int status, @Param("limit") int limit);

	@Select("<script>SELECT article_id, term_id FROM cms_article_term WHERE article_id IN "
			+ "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	@Results({ @Result(property = "articleId", column = "article_id"),
			@Result(property = "termId", column = "term_id") })
	List<ArticleTermView> selectArticleTermsByArticleIds(@Param("articleIds") List<Long> articleIds);

	@Delete("DELETE FROM cms_article_term WHERE article_id = #{articleId}")
	int deleteTermsByArticleId(@Param("articleId") Long articleId);

	@Insert("<script>INSERT INTO cms_article_term (article_id, term_id) VALUES "
			+ "<foreach collection='termIds' item='tid' separator=','>(#{articleId}, #{tid})</foreach></script>")
	int insertArticleTerms(@Param("articleId") Long articleId, @Param("termIds") List<Integer> termIds);

	@Select("<script>" + "SELECT COUNT(*) FROM cms_article "
			+ "WHERE create_time &gt;= #{start} AND create_time &lt; #{endExclusive} "
			+ "<if test='authorId != null'> AND author_id = #{authorId} </if>" + "</script>")
	long countCreatedByRange(@Param("start") LocalDateTime start, @Param("endExclusive") LocalDateTime endExclusive,
			@Param("authorId") Long authorId);

	@Select("<script>" + "SELECT " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(create_time) AS stat_day</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time) AS yearMonth</otherwise>"
			+ "</choose>, status AS status, COUNT(*) AS total " + "FROM cms_article WHERE create_time &gt;= #{start} "
			+ "<if test='authorId != null'> AND author_id = #{authorId} </if>" + "GROUP BY " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(create_time)</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time)</otherwise>" + "</choose>, status" + "</script>")
	List<StatisticEntry> countArticleByRange(@Param("g") StatisticUnit g, @Param("start") LocalDateTime start,
			@Param("authorId") Long authorId);

	@Select("<script>" + "SELECT " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(publish_time) AS stat_day</when>"
			+ "  <otherwise>YEAR(publish_time)*100+MONTH(publish_time) AS yearMonth</otherwise>"
			+ "</choose>, COUNT(*) AS total " + "FROM cms_article WHERE status = " + ArticleMapper.STATUS_PUBLISHED
			+ " AND publish_time &gt;= #{start} " + "<if test='authorId != null'> AND author_id = #{authorId} </if>"
			+ "GROUP BY " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(publish_time)</when>"
			+ "  <otherwise>YEAR(publish_time)*100+MONTH(publish_time)</otherwise>" + "</choose>" + "</script>")
	List<StatisticEntry> countPublishedByRange(@Param("g") StatisticUnit g, @Param("start") LocalDateTime start,
			@Param("authorId") Long authorId);

	@Select("<script>" + "SELECT " + "<choose>"
			+ "  <when test='g.name() == \"DAY\"'>DATE(create_time) AS stat_day</when>"
			+ "  <when test='g.name() == \"WEEK\"'>" + ArticleMapper.WEEK_BUCKET_CREATE + " AS stat_day</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time) AS yearMonth</otherwise>"
			+ "</choose>, COUNT(DISTINCT author_id) AS total " + "FROM cms_article WHERE create_time &gt;= #{start} "
			+ "GROUP BY " + "<choose>" + "  <when test='g.name() == \"DAY\"'>DATE(create_time)</when>"
			+ "  <when test='g.name() == \"WEEK\"'>stat_day</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time)</otherwise>" + "</choose>" + "</script>")
	List<StatisticEntry> countActiveAuthorsByRange(@Param("g") StatisticUnit g, @Param("start") LocalDateTime start);

	@Select("<script>" + "SELECT " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(create_time) AS stat_day</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time) AS yearMonth</otherwise>"
			+ "</choose>, category_id AS categoryId, COUNT(*) AS total "
			+ "FROM cms_article WHERE create_time &gt;= #{start} " + "GROUP BY " + "<choose>"
			+ "  <when test='g.name() == \"DAY\" or g.name() == \"WEEK\"'>DATE(create_time)</when>"
			+ "  <otherwise>YEAR(create_time)*100+MONTH(create_time)</otherwise>" + "</choose>, category_id"
			+ "</script>")
	List<StatisticEntry> countCategoryByRange(@Param("g") StatisticUnit g, @Param("start") LocalDateTime start);
}
