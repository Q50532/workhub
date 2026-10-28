package com.example.workhub.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;
import com.example.workhub.model.TicketAuditLog;

@Mapper
public interface TicketAuditMapper {
    @Insert("""
        INSERT INTO ticket_audit_logs(ticket_id, actor_id, action, old_value, new_value)
        VALUES(#{ticketId}, #{actorId}, #{action}, #{oldValue}, #{newValue})
        """)
    int add(TicketAuditLog log);

    @Select("""
        SELECT * FROM ticket_audit_logs WHERE ticket_id = #{ticketId}
        ORDER BY id ASC
        """)
    List<TicketAuditLog> findByTicketId(@Param("ticketId") int ticketId);
}
