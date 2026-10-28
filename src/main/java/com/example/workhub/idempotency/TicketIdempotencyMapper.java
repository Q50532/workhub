package com.example.workhub.idempotency;

import org.apache.ibatis.annotations.*;

@Mapper
public interface TicketIdempotencyMapper {
    /** Duplicate request waits for the original SQL transaction to commit/rollback. */
    @Insert("""
        INSERT INTO ticket_create_requests(actor_id, request_key, request_hash)
        VALUES(#{actorId}, #{requestKey}, #{requestHash})
        ON DUPLICATE KEY UPDATE request_key = request_key
        """)
    int reserve(@Param("actorId") int actorId, @Param("requestKey") String requestKey,
                @Param("requestHash") String requestHash);

    @Select("""
        SELECT actor_id, request_key, request_hash, ticket_id
        FROM ticket_create_requests
        WHERE actor_id = #{actorId} AND request_key = #{requestKey} FOR UPDATE
        """)
    TicketIdempotencyRecord findForUpdate(@Param("actorId") int actorId,
                                          @Param("requestKey") String requestKey);

    @Update("""
        UPDATE ticket_create_requests SET ticket_id = #{ticketId}
        WHERE actor_id = #{actorId} AND request_key = #{requestKey} AND ticket_id IS NULL
        """)
    int complete(@Param("actorId") int actorId, @Param("requestKey") String requestKey,
                 @Param("ticketId") int ticketId);
}
