package com.example.workhub.mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.example.workhub.model.Ticket;

@Mapper
public interface TicketMapper {

    @Insert("""
        INSERT INTO tickets
            (title, description, category, created_by, assigned_to)
        VALUES
            (#{title}, #{description}, #{category}, #{createdBy}, #{assignedTo})
        """)
    int add(Ticket ticket);
}
