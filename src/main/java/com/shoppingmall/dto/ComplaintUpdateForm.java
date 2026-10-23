package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Size;

@Getter @Setter
public class ComplaintUpdateForm {
    private Long assigneeId;
    @Size(max = 1000)
    private String resolutionNote;
}
