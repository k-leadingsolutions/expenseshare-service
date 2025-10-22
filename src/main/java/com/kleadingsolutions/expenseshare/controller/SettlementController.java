package com.kleadingsolutions.expenseshare.controller;

import com.kleadingsolutions.expenseshare.dto.SettlementRequest;
import com.kleadingsolutions.expenseshare.model.Settlement;
import com.kleadingsolutions.expenseshare.service.AuthService;
import com.kleadingsolutions.expenseshare.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<Settlement> settle(@RequestBody SettlementRequest request) {
        var initiatedBy = authService.getCurrentUserId();
        Settlement s = settlementService.settle(request.getGroupId(), request.getPayerId(), request.getReceiverId(), request.getAmount(), initiatedBy);
        return ResponseEntity.ok(s);
    }
}