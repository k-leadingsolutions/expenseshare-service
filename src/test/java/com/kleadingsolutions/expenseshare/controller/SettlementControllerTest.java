package com.kleadingsolutions.expenseshare.controller;

import com.kleadingsolutions.expenseshare.dto.SettlementRequest;
import com.kleadingsolutions.expenseshare.model.Settlement;
import com.kleadingsolutions.expenseshare.service.AuthService;
import com.kleadingsolutions.expenseshare.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    @Mock
    private SettlementService settlementService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private SettlementController settlementController;

    private UUID initiator;
    private UUID groupId;
    private UUID payerId;
    private UUID receiverId;

    @BeforeEach
    void setUp() {
        initiator = UUID.randomUUID();
        groupId = UUID.randomUUID();
        payerId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
    }

    @Test
    void settle_shouldReturnSettlement() {
        SettlementRequest req = org.mockito.Mockito.mock(SettlementRequest.class);
        when(req.getGroupId()).thenReturn(groupId);
        when(req.getPayerId()).thenReturn(payerId);
        when(req.getReceiverId()).thenReturn(receiverId);
        when(req.getAmount()).thenReturn(new BigDecimal("12.34"));

        Settlement s = org.mockito.Mockito.mock(Settlement.class);

        when(authService.getCurrentUserId()).thenReturn(initiator);
        when(settlementService.settle(eq(groupId), eq(payerId), eq(receiverId), any(), eq(initiator))).thenReturn(s);

        ResponseEntity<Settlement> resp = settlementController.settle(req);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        assertSame(s, resp.getBody());
    }
}