package com.campus.activity.activity.enums;

public enum RegistrationStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    CANCELED("已取消"),
    COMPLETED("已完成");

    private final String description;

    RegistrationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
