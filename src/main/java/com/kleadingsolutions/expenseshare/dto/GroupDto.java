package com.kleadingsolutions.expenseshare.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDto {
    private UUID id;
    private String name;
    private UUID createdBy;
    private LocalDateTime createdAt;
}