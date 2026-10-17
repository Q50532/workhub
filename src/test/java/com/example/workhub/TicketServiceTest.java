package com.example.workhub;

import com.example.workhub.service.TicketService;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.mapper.TicketLogMapper;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.Ticket;
import com.example.workhub.model.TicketLog;
import com.example.workhub.exception.InvalidStatusTransitionException;
import com.example.workhub.exception.TicketNotFoundException;

import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketServiceTest {

    private TicketMapper ticketMapper;
    private TicketLogMapper ticketLogMapper;
    private UserMapper userMapper;
    private StringRedisTemplate redisTemplate;
    private JsonMapper jsonMapper;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketMapper = Mockito.mock(TicketMapper.class);
        ticketLogMapper = Mockito.mock(TicketLogMapper.class);
        userMapper = Mockito.mock(UserMapper.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        jsonMapper = Mockito.mock(JsonMapper.class);

        ticketService = new TicketService(
                ticketMapper,
                ticketLogMapper,
                userMapper,
                redisTemplate,
                jsonMapper
        );
    }

    @ParameterizedTest
    @CsvSource({
        "OPEN, IN_PROGRESS",
        "IN_PROGRESS, RESOLVED",
        "RESOLVED, CLOSED",
        "RESOLVED, IN_PROGRESS"
    })
    void validStatusTransitionShouldPass(String currentStatus, String newStatus) {

        Ticket ticket = new Ticket();
        ticket.setStatus(currentStatus);

        Mockito.when(ticketMapper.findById(1))
                .thenReturn(ticket);

        Mockito.when(ticketMapper.updateStatus(1, newStatus))
                .thenReturn(1);

        int result = ticketService.updateStatus(1, newStatus);

        assertEquals(1, result);

        Mockito.verify(ticketMapper)
                .updateStatus(1, newStatus);

        Mockito.verify(ticketLogMapper)
                .add(Mockito.any(TicketLog.class));

        Mockito.verify(redisTemplate)
                .delete("ticket:1");
    }

    @Test
    void openToClosedShouldBeRejected() {
        Ticket ticket = new Ticket();
        ticket.setStatus("OPEN");

        Mockito.when(ticketMapper.findById(1))
                .thenReturn(ticket);

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> ticketService.updateStatus(1, "CLOSED")
        );

        Mockito.verify(ticketMapper, Mockito.never())
                .updateStatus(1, "CLOSED");
    }

    @Test
    void ticketNotFoundShouldThrowException() {
        Mockito.when(ticketMapper.findById(999))
                .thenReturn(null);

        assertThrows(
                TicketNotFoundException.class,
                () -> ticketService.updateStatus(999, "IN_PROGRESS")
        );

        Mockito.verify(ticketMapper, Mockito.never())
                .updateStatus(999, "IN_PROGRESS");
    }
}