package com.kleadingsolutions.expenseshare.mapper;

import com.kleadingsolutions.expenseshare.dto.GroupDto;
import com.kleadingsolutions.expenseshare.dto.CreateGroupRequest;
import com.kleadingsolutions.expenseshare.model.Group;

public final class GroupMapper {
    private GroupMapper() {}

    public static GroupDto toDto(Group g) {
        if (g == null) return null;
        return GroupDto.builder()
                .id(g.getId())
                .name(g.getName())
                .createdBy(g.getCreatedBy())
                .createdAt(g.getCreatedAt())
                .build();
    }

    public static Group fromCreateRequest(CreateGroupRequest req) {
        if (req == null) return null;
        Group g = new Group();
        g.setName(req.getName());
        return g;
    }
}