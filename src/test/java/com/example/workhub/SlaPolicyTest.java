package com.example.workhub;

import com.example.workhub.sla.SlaPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SlaPolicyTest {
    @Test void deadlinesArePrioritySpecific() {
        assertEquals(60, SlaPolicy.minutesFor("URGENT"));
        assertEquals(240, SlaPolicy.minutesFor("HIGH"));
        assertEquals(1440, SlaPolicy.minutesFor("MEDIUM"));
        assertEquals(4320, SlaPolicy.minutesFor("LOW"));
        assertThrows(IllegalArgumentException.class, () -> SlaPolicy.minutesFor("BAD"));
    }
}
