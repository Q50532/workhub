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
    int add(Ticket ticket);

    @Select("""
            Select * from tickets

            """)
    List<Ticket> findAll();

    @Select("""
            Select * from tickets where id=#{id}
            """)
    Ticket findById(int id);

    @Select("""
        SELECT * FROM tickets
        WHERE status = #{status}
        """)
    List<Ticket> findByStatus(String status);

    @Update("""
        update tickets
        Set
            title = #{ticket.title},
            description = #{ticket.description},
            category = #{ticket.category},
            assigned_to = #{ticket.assignedTo},
            status = #{ticket.status},
            resolution = #{ticket.resolution}
        where id = #{id}
            """)
    int update(
        @Param ("id") int id,
        @Param ("ticket") Ticket ticket
    );

    @Update("""
            update tickets
            Set
                status=#{status}
            where id=#{id}
            """)
    int updateStatus(
        @Param ("id") int id,
        @Param ("status") String status
    );


    @Update("""
            update tickets
            Set
                assigned_to=#{assignedTo}
            where id=#{id}
            """)
    int updateAssignee(@Param("id") int id, @Param("assignedTo") Integer assignedTo);

    @Update("""
            update tickets
            Set
                priority=#{priority}
            where id=#{id}
            """)
    int updatePriority(@Param("id") int id,@Param("priority") String priority);

    @Delete("""
            Delete from tickets
            where id = #{id}
            """)
    int delete(int id);
}

