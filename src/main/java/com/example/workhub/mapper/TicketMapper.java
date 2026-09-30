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

    @Delete("""
            Delete from tickets
            where id = #{id}
            """)
    int delete(int id);
}

