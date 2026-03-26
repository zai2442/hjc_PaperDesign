package com.campus.activity.common;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class IdListRequest {
    @NotEmpty(message = "ids is required")
    private List<Long> ids;
}
