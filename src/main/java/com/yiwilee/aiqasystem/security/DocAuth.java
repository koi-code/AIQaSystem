package com.yiwilee.aiqasystem.security;

import com.yiwilee.aiqasystem.repository.DocumentRepo;
import com.yiwilee.aiqasystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 知识库文档专属的安全校验组件
 * 用于 @PreAuthorize 注解中的自定义 SpEL 表达式鉴权
 */
@Slf4j
@Component("docAuth") // 声明 Bean 名字为 docAuth
@RequiredArgsConstructor
public class DocAuth {

    private final DocumentRepo documentRepo;

    /**
     * 判断当前登录用户是否是该文档的上传者 (Owner)
     */
    public boolean isOwner(Long documentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 查库比对上传者 ID 与当前登录者 ID
        // 注意：这里的 getUploaderId() 请根据你 Document 实体类中实际的属性名进行调整
        return documentRepo.findById(documentId)
                .map(doc -> doc.getUploader().getId().equals(currentUserId))
                .orElse(false); // 如果文档不存在，直接返回 false，触发 403
    }
}