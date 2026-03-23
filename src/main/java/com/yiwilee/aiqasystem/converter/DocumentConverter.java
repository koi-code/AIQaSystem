package com.yiwilee.aiqasystem.converter;

import com.yiwilee.aiqasystem.model.entity.Document;
import com.yiwilee.aiqasystem.model.entity.DocumentChunk;
import com.yiwilee.aiqasystem.model.vo.DocumentChunkVO;
import com.yiwilee.aiqasystem.model.vo.DocumentVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentConverter {

    // ==========================================
    // Document (文档主表) 转换
    // ==========================================
    public DocumentVO toVO(Document document) {
        if (document == null) {
            return null;
        }

        // 扁平化提取上传者的用户名，防止级联引起的懒加载异常或死循环
        String uploaderName = "未知用户";
        if (document.getUploader() != null) {
            uploaderName = document.getUploader().getUsername();
        }

        return new DocumentVO(
                document.getId(),
                document.getName(),
                document.getOriginalName(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus(),
                document.getSummary(),
                document.getErrorMsg(),
                uploaderName,
                document.getAllowedRoles(),
                document.getCreateTime()
        );
    }

    public List<DocumentVO> toVOList(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // DocumentChunk (文档分块) 转换
    // ==========================================
    public DocumentChunkVO toChunkVO(DocumentChunk chunk) {
        if (chunk == null) {
            return null;
        }
        return new DocumentChunkVO(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getPageNum(),
                chunk.getContent()
        );
    }

    public List<DocumentChunkVO> toChunkVOList(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        return chunks.stream()
                .map(this::toChunkVO)
                .collect(Collectors.toList());
    }
}