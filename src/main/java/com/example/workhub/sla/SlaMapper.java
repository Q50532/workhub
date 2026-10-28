package com.example.workhub.sla;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SlaMapper {
    @Insert("""
        INSERT INTO ticket_sla (ticket_id, due_at)
        VALUES (#{ticketId}, DATE_ADD(NOW(), INTERVAL #{minutes} MINUTE))
        """)
    int create(@Param("ticketId") int ticketId, @Param("minutes") int minutes);

    @Update("""
        UPDATE ticket_sla SET due_at = DATE_ADD(NOW(), INTERVAL #{minutes} MINUTE),
            escalated_at = NULL
        WHERE ticket_id = #{ticketId}
        """)
    int reset(@Param("ticketId") int ticketId, @Param("minutes") int minutes);

    @Select("""
        SELECT s.ticket_id FROM ticket_sla s
        JOIN tickets t ON t.id = s.ticket_id
        WHERE s.escalated_at IS NULL AND s.due_at <= NOW()
          AND t.deleted = FALSE AND t.status IN ('OPEN','IN_PROGRESS')
        ORDER BY s.due_at ASC LIMIT #{maxRows}
        """)
    List<Integer> overdue(@Param("maxRows") int maxRows);

    @Select("SELECT ticket_id, due_at, escalated_at FROM ticket_sla WHERE ticket_id = #{ticketId}")
    SlaStatus findStatus(@Param("ticketId") int ticketId);

    /** Atomic claim conditioned on Ticket status: no duplicate escalation across app replicas. */
    @Update("""
        UPDATE ticket_sla s JOIN tickets t ON t.id = s.ticket_id
        SET s.escalated_at = NOW()
        WHERE s.ticket_id = #{ticketId} AND s.escalated_at IS NULL
          AND s.due_at <= NOW() AND t.deleted = FALSE
          AND t.status IN ('OPEN','IN_PROGRESS')
        """)
    int markEscalated(@Param("ticketId") int ticketId);
}
