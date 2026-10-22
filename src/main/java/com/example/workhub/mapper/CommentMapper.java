package com.example.workhub.mapper;

import org.apache.ibatis.annotations.*;

import com.example.workhub.model.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("""
        Insert Into comments
        (ticket_id, author_id, content)
        Values
        (#{ticketId}, #{authorId}, #{content})
        """)
    int add(Comment comment);

    

    @Select("""
        SELECT id, ticket_id, author_id, content,
            created_at, is_resolution AS resolution
        FROM comments
        WHERE ticket_id = #{ticketId}
        """)
    List<Comment> findByTicketId(int ticketId);

    @Select("""
        SELECT COUNT(*) FROM comments
        WHERE id = #{commentId} AND ticket_id = #{ticketId}
        """)
    int countCommentInTicket(
        @Param("ticketId") int ticketId,
        @Param("commentId") int commentId
    );

    @Update("""
        UPDATE comments
        SET is_resolution = 0
        WHERE ticket_id = #{ticketId}
        AND is_resolution = 1
        """)
    int clearResolutionByTicketId(int ticketId);

    @Update("""
        UPDATE comments
        SET is_resolution = 1
        WHERE id = #{commentId} AND ticket_id = #{ticketId}
        """)
    int setResolution(
        @Param("ticketId") int ticketId,
        @Param("commentId") int commentId
    );

    @Update("""
        UPDATE comments
        SET is_resolution = 0
        WHERE ticket_id = #{ticketId}
        AND id = #{commentId}
        AND is_resolution = 1
        """)
    int unmarkResolution(
        @Param("ticketId") int ticketId,
        @Param("commentId") int commentId
    );

    
}
