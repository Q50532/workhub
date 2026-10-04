package com.example.workhub.mapper;
import org.apache.ibatis.annotations.*;
import com.example.workhub.model.TicketLog;

import java.util.List;

@Mapper
public interface TicketLogMapper {

    @Insert("""
            Insert Into ticket_logs
            (ticket_id, old_status, new_status)
            Values
            (#{ticketId}, #{oldStatus},#{newStatus})
            """)
    int add (TicketLog ticketLog);
    
    @Select(
        """
            Select * from ticket_logs
            where ticket_id=#{ticketId}
            Order By created_at
                """
    )
    List<TicketLog> findByTicketId(int ticketId);
}
