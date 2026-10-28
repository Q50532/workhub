package com.example.workhub.mapper;
import org.apache.ibatis.annotations.*;

import com.example.workhub.model.Ticket;

import java.util.List;

@Mapper
public interface TicketMapper {

    @Insert("""
        INSERT INTO tickets
            (title, description, category, created_by, assigned_to)
        VALUES
            (#{title}, #{description}, #{category}, #{createdBy}, #{assignedTo})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int add(Ticket ticket);

    @Select("""
            Select * from tickets
            Where deleted=FALSE

            """)
    List<Ticket> findAll();

    @Select("""
            Select * from tickets where id=#{id}
            And deleted=FALSE
            """)
    Ticket findById(int id);

    @Select("""
        SELECT * FROM tickets
        WHERE status = #{status}
            And deleted=FALSE
        """)
    List<Ticket> findByStatus(String status);

    @Select("""
            Select * from tickets
            where priority=#{priority}
            And deleted=FALSE
            """)
    List<Ticket> findByPriority(String priority);

    @Select("""
        SELECT * FROM tickets
        WHERE assigned_to = #{assignedTo}
            And deleted=FALSE
        """)
    List<Ticket> findByAssignedTo(Integer assignedTo);

    @Select("""
        <script>
        SELECT * FROM tickets
        WHERE deleted = FALSE

        <if test="status != null">
            AND status = #{status}
        </if>

        <if test="priority != null">
            AND priority = #{priority}
        </if>

        <if test="assignedTo != null">
            AND assigned_to = #{assignedTo}
        </if>

        <if test="viewerId != null">
            AND (created_by = #{viewerId} OR assigned_to = #{viewerId})
        </if>

        ORDER BY
        <choose>
            <when test="sortBy == 'createdAt'">
                created_at
            </when>
            <otherwise>
                id
            </otherwise>
        </choose>

        <choose>
            <when test="order == 'desc'">
                DESC
            </when>
            <otherwise>
                ASC
            </otherwise>
        </choose>
        /* If the primary sort values are equal, sort by ID. */
        ,id ASC 
        
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<Ticket> findByFilters(
        @Param("status") String status,
        @Param("priority") String priority,
        @Param("assignedTo") Integer assignedTo,
        @Param("limit") int limit,
        @Param("offset") int offset,
        @Param("sortBy") String sortBy,
        @Param("order") String order,
        @Param("viewerId") Integer viewerId
    );

    @Select("""
        <script>
        SELECT COUNT(*) FROM tickets
        WHERE deleted = FALSE

        <if test="status != null">
            AND status = #{status}
        </if>

        <if test="priority != null">
            AND priority = #{priority}
        </if>

        <if test="assignedTo != null">
            AND assigned_to = #{assignedTo}
        </if>

        <if test="viewerId != null">
            AND (created_by = #{viewerId} OR assigned_to = #{viewerId})
        </if>

        </script>
        """)
    int countByFilters(
        @Param("status") String status,
        @Param("priority") String priority,
        @Param("assignedTo") Integer assignedTo,
        @Param("viewerId") Integer viewerId
    );

    @Update("""
        update tickets
        Set
            title = #{ticket.title},
            description = #{ticket.description},
            category = #{ticket.category},            
            resolution = #{ticket.resolution},
            version = version + 1
        where id = #{id} AND deleted = FALSE
          AND version = #{version}
            """)
    int update(
        @Param ("id") int id,
        @Param ("ticket") Ticket ticket,
        @Param("version") int version
    );

    @Update("""
            update tickets
            Set
                status=#{status},
                version=version+1
            where id=#{id} AND deleted=FALSE
              AND status=#{expectedStatus} AND version=#{version}
            """)
    int updateStatus(
        @Param ("id") int id,
        @Param ("status") String status,
        @Param("expectedStatus") String expectedStatus,
        @Param("version") int version
    );


    @Update("""
            update tickets
            Set
                assigned_to=#{assignedTo},
                version=version+1
            where id=#{id} AND deleted=FALSE AND version=#{version}
            """)
    int updateAssignee(@Param("id") int id, @Param("assignedTo") Integer assignedTo,
                       @Param("version") int version);

    @Update("""
            update tickets
            Set
                priority=#{priority},
                version=version+1
            where id=#{id} AND deleted=FALSE AND version=#{version}
            """)
    int updatePriority(@Param("id") int id,@Param("priority") String priority,
                       @Param("version") int version);

    @Update("""
        UPDATE tickets
        SET deleted = TRUE, version = version + 1
        WHERE id = #{id}
        AND deleted = FALSE AND version = #{version}
        """)
    int delete(@Param("id") int id, @Param("version") int version);
}

