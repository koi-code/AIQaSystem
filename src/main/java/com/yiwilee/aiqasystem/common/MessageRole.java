package com.yiwilee.aiqasystem.common;

import lombok.Getter;

@Getter
public enum MessageRole {
    USER("USER"), SYSTEM("SYSTEM"), ASSISTANT("ASSISTANT");
    private final String role;
    MessageRole(String role) {
        this.role = role;
    }

}
