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
package top.ruilink.inkwash.system.mapper;

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

import top.ruilink.inkwash.system.api.query.NoticeQuery;
import top.ruilink.inkwash.system.domain.SysNotice;

/**
 * Notice persistence mapper.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Mapper
public interface NoticeMapper {

	@Results(id = "baseResult", value = { @Result(property = "id", column = "id"),
			@Result(property = "title", column = "title"), @Result(property = "type", column = "type"),
			@Result(property = "content", column = "content"), @Result(property = "status", column = "status"),
			@Result(property = "recipientId", column = "recipient_id"),
			@Result(property = "readTime", column = "read_time"), @Result(property = "creator", column = "creator"),
			@Result(property = "createTime", column = "create_time"), @Result(property = "updater", column = "updater"),
			@Result(property = "updateTime", column = "update_time") })
	@Select("SELECT * FROM sys_notice WHERE id = #{id}")
	SysNotice selectById(Long id);

	@Select("<script>SELECT * FROM sys_notice <where><if test='type != null and type != \"\"'>AND type = #{type}</if><if test='status != null and status != \"\"'>AND status = #{status}</if></where> ORDER BY create_time DESC</script>")
	@ResultMap("baseResult")
	List<SysNotice> selectList(@Param("type") String type, @Param("status") String status);

	@Select("<script>SELECT * FROM sys_notice <where><if test='type != null and type != \"\"'>AND type = #{type}</if><if test='status != null and status != \"\"'>AND status = #{status}</if></where> ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}</script>")
	@ResultMap("baseResult")
	List<SysNotice> selectPage(@Param("type") String type, @Param("status") String status, @Param("offset") int offset,
			@Param("limit") int limit);

	@Select("<script>SELECT COUNT(*) FROM sys_notice <where><if test='type != null and type != \"\"'>AND type = #{type}</if><if test='status != null and status != \"\"'>AND status = #{status}</if></where></script>")
	long count(@Param("type") String type, @Param("status") String status);

	@Select("<script>" + "SELECT COUNT(*) FROM sys_notice " + "<where>"
			+ "  <if test='query.type != null'> AND type = #{query.type} </if>"
			+ "  <if test='query.status != null and query.status != \"\"'> AND status = #{query.status} </if>"
			+ "</where>" + "</script>")
	long countNotices(@Param("query") NoticeQuery query);

	@ResultMap("baseResult")
	@Select("<script>" + "SELECT * FROM sys_notice " + "<where>"
			+ "  <if test='query.type != null'> AND type = #{query.type} </if>"
			+ "  <if test='query.status != null and query.status != \"\"'> AND status = #{query.status} </if>"
			+ "</where>" + "<if test='sortSql != null'> ORDER BY ${sortSql} </if>" + "LIMIT #{limit} OFFSET #{offset}"
			+ "</script>")
	List<SysNotice> selectNoticeList(@Param("query") NoticeQuery query, @Param("offset") long offset,
			@Param("limit") int limit, @Param("sortSql") String sortSql);

	@Insert("INSERT INTO sys_notice (title, type, content, status, recipient_id, creator, create_time, updater, update_time) VALUES (#{title}, #{type}, #{content}, #{status}, #{recipientId}, #{creator}, #{createTime}, #{updater}, #{updateTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	Long create(SysNotice notice);

	/**
	 * Inserts many notices in one round trip.
	 *
	 * <p>
	 * Replaces a per-recipient INSERT loop when a single article action fans out to
	 * every editor (see {@code ArticleNoticeEventListener.notifyEditors}). All rows
	 * are written inside the caller's transaction, so batching changes the
	 * statement count, not the atomicity.
	 *
	 * <p>
	 * An empty list is a no-op — the {@code <if>} guard prevents MyBatis from
	 * emitting an {@code INSERT} with an empty value list, which is a syntax error.
	 *
	 * @param notices notices to insert; generated ids are not populated
	 * @return number of rows inserted
	 */
	@Insert("<script>INSERT INTO sys_notice (title, type, content, status, recipient_id, creator, create_time, updater, update_time) VALUES "
			+ "<foreach collection='list' item='n' separator=','>"
			+ "(#{n.title}, #{n.type}, #{n.content}, #{n.status}, #{n.recipientId}, #{n.creator}, #{n.createTime}, #{n.updater}, #{n.updateTime})"
			+ "</foreach></script>")
	int createBatch(@Param("list") List<SysNotice> notices);

	/**
	 * Batch insert with an empty-collection guard.
	 *
	 * <p>
	 * The annotation SQL cannot guard itself: with an empty {@code list} the
	 * {@code <foreach>} emits no tuples and MyBatis still opens the statement,
	 * producing {@code INSERT INTO ... VALUES } and a syntax error. Callers must
	 * therefore return early on an empty collection; this helper does that so the
	 * trap is not repeated at every call site.
	 *
	 * @param notices notices to insert; generated ids are not populated
	 * @return number of rows inserted, or {@code 0} without touching the database
	 */
	default int createBatchIfAny(List<SysNotice> notices) {
		if (notices == null || notices.isEmpty()) {
			return 0;
		}
		return createBatch(notices);
	}

	@Update("<script>UPDATE sys_notice <set><if test='title != null'>title = #{title},</if><if test='type != null'>type = #{type},</if><if test='content != null'>content = #{content},</if><if test='status != null'>status = #{status},</if><if test='updater != null'>updater = #{updater},</if>update_time = #{updateTime}</set> WHERE id = #{id}</script>")
	int update(SysNotice notice);

	@Delete("DELETE FROM sys_notice WHERE id = #{id}")
	int deleteById(Long id);

	@Delete("<script>DELETE FROM sys_notice WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	int deleteByIds(@Param("ids") List<Long> ids);

	@Select("SELECT * FROM sys_notice WHERE status = '0' AND (recipient_id IS NULL OR recipient_id = #{userId}) ORDER BY create_time DESC LIMIT 100")
	@ResultMap("baseResult")
	List<SysNotice> selectForUser(@Param("userId") Long userId);

	@Select("SELECT COUNT(*) FROM sys_notice WHERE status = '0' AND recipient_id = #{userId} AND read_time IS NULL")
	long unreadCount(@Param("userId") Long userId);

	@Update("<script>UPDATE sys_notice SET read_time = NOW() WHERE recipient_id = #{userId} AND id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	int markRead(@Param("ids") List<Long> ids, @Param("userId") Long userId);

	@Update("UPDATE sys_notice SET read_time = NOW() WHERE recipient_id = #{userId} AND read_time IS NULL")
	int markAllRead(@Param("userId") Long userId);

	@Delete("<script>DELETE FROM sys_notice WHERE recipient_id = #{userId} AND id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
	int deleteOwn(@Param("ids") List<Long> ids, @Param("userId") Long userId);

	@Delete("DELETE FROM sys_notice WHERE recipient_id = #{userId}")
	int clearOwn(@Param("userId") Long userId);
}
