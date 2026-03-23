package com.yiwilee.aiqasystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSessionUpdateDTO(
        @NotBlank(message = "会话标题不能为空")
        @Size(max = 100, message = "标题长度不能超过100个字符")
        String newTitle
) {}