package com.yiwilee.aiqasystem.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yiwilee.aiqasystem.converter.DocumentConverter;
import com.yiwilee.aiqasystem.exception.DocumentException;
import com.yiwilee.aiqasystem.exception.ResourceNotFoundException;
import com.yiwilee.aiqasystem.model.entity.Document;
import com.yiwilee.aiqasystem.model.entity.DocumentChunk;
import com.yiwilee.aiqasystem.model.entity.SysUser;
import com.yiwilee.aiqasystem.model.vo.DocumentChunkVO;
import com.yiwilee.aiqasystem.model.vo.DocumentVO;
import com.yiwilee.aiqasystem.repository.DocumentChunkRepo;
import com.yiwilee.aiqasystem.repository.DocumentRepo;
import com.yiwilee.aiqasystem.repository.UserRepo;
import com.yiwilee.aiqasystem.service.DocumentService;
import com.yiwilee.aiqasystem.service.VectorService;
import com.yiwilee.aiqasystem.util.DocumentParser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepo documentRepo;
    private final DocumentChunkRepo documentChunkRepo;
    private final UserRepo userRepo;

    private final DocumentParser documentParser;
    private final DocumentConverter documentConverter;

    private final VectorService vectorService;
    private final EmbeddingModel embeddingModel;

    @Value("${aiqa.upload.path:/tmp/aiqa/uploads/}")
    private String uploadPath;

    // 警告：这里刻意去掉了最外层的 @Transactional！
    // 因为解析文件、调用大模型 Embedding 和写入 Milvus 都是耗时的 IO 操作。
    // 如果套上事务，会导致数据库连接池被长久占用，引发连接泄露甚至系统雪崩。
    @Override
    public DocumentVO uploadAndParseDocument(MultipartFile file, List<String> allowedRoles, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new DocumentException("上传的文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String fileType = StrUtil.blankToDefault(FileUtil.extName(originalName), "txt").toLowerCase();
        String name = FileUtil.mainName(originalName);

        SysUser uploader = userRepo.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("上传者不存在 (ID: " + uploaderId + ")"));

        // 1. 物理保存文件 (本地硬盘)
        String filePath = saveFileToDisk(file, fileType);

        // 2. 构建主表记录并落库 (短平快的 DB 操作)
        Document document = Document.builder()
                .name(name)
                .originalName(originalName)
                .filePath(filePath)
                .fileType(fileType)
                .fileSize(file.getSize())
                .status(Document.STATUS_PROCESSING)
                .uploader(uploader)
                .allowedRoles(allowedRoles == null ? new ArrayList<>() : allowedRoles)
                .build();
        document = documentRepo.save(document);

        // 3. 开始解析与向量化 (耗时操作，脱离事务保护)
        try {
            log.info("开始解析文件: {}", originalName);
            DocumentParser.ParseResult parseResult = documentParser.parseWithPages(filePath);
            List<DocumentParser.PageContent> pages = parseResult.pages();

            List<DocumentChunk> chunksToSave = new ArrayList<>();
            int index = 0;
            for (DocumentParser.PageContent page : pages) {
                chunksToSave.add(DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(index++)
                        .pageNum(page.pageNum())
                        .content(page.content())
                        .build());
            }

            // 批量保存分块到 MySQL
            List<DocumentChunk> savedChunks = documentChunkRepo.saveAll(chunksToSave);
            log.info("MySQL 数据块保存完毕 (数量:{})，准备生成向量并写入 Milvus...", savedChunks.size());

            // 逐个切片请求大模型并写入 Milvus
            for (DocumentChunk chunk : savedChunks) {
                try {
                    float[] vector = embeddingModel.embed(chunk.getContent());
                    vectorService.insertVector(chunk.getId(), vector, "chunk", document.getAllowedRoles());

                    // 防并发限流，保护外部 API
                    Thread.sleep(200);
                } catch (Exception e) {
                    log.error("Chunk ID: {} 向量化或写入 Milvus 失败: {}", chunk.getId(), e.getMessage());
                }
            }

            // 更新最终状态为成功
            document.setStatus(Document.STATUS_COMPLETED);
            log.info("文件 [{}] 解析与向量化全部完成", originalName);

        } catch (Exception e) {
            log.error("文件解析发生致命异常", e);
            document.setStatus(Document.STATUS_FAILED);
            document.setErrorMsg(e.getMessage());
        }

        // 再次落库保存最终状态
        return documentConverter.toVO(documentRepo.save(document));
    }

    private String saveFileToDisk(MultipartFile file, String extName) {
        try {
            String newFileName = IdUtil.simpleUUID() + "." + extName;
            File destDir = new File(uploadPath).getAbsoluteFile();
            File destFile = new File(destDir, newFileName);

            if (!destDir.exists() && !destDir.mkdirs()) {
                throw new DocumentException("创建上传目录失败: " + destDir.getAbsolutePath());
            }

            Files.copy(file.getInputStream(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("文件保存到物理硬盘失败", e);
            throw new DocumentException("文件物理保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentVO> pageDocuments(String keyword, Integer status, int pageNum, int pageSize) {
        int actualPageNum = Math.max(0, pageNum - 1);
        Pageable pageable = PageRequest.of(actualPageNum, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Specification<Document> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StrUtil.isNotBlank(keyword)) {
                String likePattern = "%" + keyword.trim() + "%";
                Predicate nameLike = cb.like(root.get("name"), likePattern);
                Predicate originalNameLike = cb.like(root.get("originalName"), likePattern);
                predicates.add(cb.or(nameLike, originalNameLike));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return documentRepo.findAll(spec, pageable).map(documentConverter::toVO);
    }

    // ==========================================
    // 新增：获取用户的文档列表 (带水平越权防护)
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentVO> pageDocumentsByUserId(Long userId, int pageNum, int pageSize) {
        int actualPage = Math.max(0, pageNum - 1);
        Pageable pageable = PageRequest.of(actualPage, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Document> docPage;
        if (userId == null) {
            // 管理员没传 ID，查询全系统文档
            docPage = documentRepo.findAll(pageable);
        } else {
            // 查询指定用户的文档
            docPage = documentRepo.findByUploaderId(userId, pageable);
        }

        return docPage.map(documentConverter::toVO);
    }

    // ==========================================
    // 💡 附加实现 1：获取单文档详情
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public DocumentVO getDocumentById(Long documentId) {
        Document document = getDocument(documentId);
        return documentConverter.toVO(document);
    }

    // ==========================================
    // 💡 附加实现 2：获取文档切片(用于 RAG 调试)
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkVO> getDocumentChunks(Long documentId) {
        Document document = getDocument(documentId);

        List<DocumentChunk> chunks = documentChunkRepo.findByDocumentId(documentId);
        return documentConverter.toChunkVOList(chunks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long documentId) {
        Document document = getDocument(documentId);

        List<DocumentChunk> chunks = documentChunkRepo.findByDocumentId(documentId);
        List<Long> chunkIds = chunks.stream().map(DocumentChunk::getId).collect(Collectors.toList());

        // 1. 删 Milvus 向量
        if (!chunkIds.isEmpty()) {
            vectorService.deleteByChunkIds(chunkIds);
        }

        // 2. 删 MySQL 数据
        documentChunkRepo.deleteAll(chunks);
        documentRepo.delete(document);

        // 3. 删物理硬盘文件，防止磁盘被打爆
        if (StrUtil.isNotBlank(document.getFilePath())) {
            boolean isDeleted = FileUtil.del(document.getFilePath());
            log.info("物理文件 {} 删除状态: {}", document.getFilePath(), isDeleted);
        }

        log.info("用户彻底删除了文件: {}", document.getName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDocuments(Long userId) {
        List<Document> documents = documentRepo.findByUserId(userId);
        if (documents.isEmpty()) {
            return 0;
        }

        List<Long> documentIds = documents.stream().map(Document::getId).collect(Collectors.toList());
        List<DocumentChunk> allChunks = documentChunkRepo.findByDocumentIdIn(documentIds);
        List<Long> allChunkIds = allChunks.stream().map(DocumentChunk::getId).collect(Collectors.toList());

        // 批量清理 Milvus & MySQL
        if (!allChunkIds.isEmpty()) {
            vectorService.deleteByChunkIds(allChunkIds);
        }
        documentChunkRepo.deleteAll(allChunks);
        documentRepo.deleteAll(documents);

        // 批量清理硬盘物理文件
        for (Document doc : documents) {
            if (StrUtil.isNotBlank(doc.getFilePath())) {
                FileUtil.del(doc.getFilePath());
            }
        }

        log.info("注销/清空操作: 共彻底删除了用户 {} 的 {} 个文档", userId, documents.size());
        return documents.size();
    }

    @Override
    public void reparseDocument(Long documentId) {
        log.warn("重试解析功能暂未实现，文档 ID: {}", documentId);
        throw new DocumentException("暂未实现该功能");
    }

    private Document getDocument(Long documentId) {
        return documentRepo.findById(documentId)
                .orElseThrow(() -> new DocumentException("知识库文件不存在！"));
    }

    /**
     * 内部安全校验工具方法
     */
    @Deprecated
    private void checkDocumentAccessAuth(Document document, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && !document.getUploader().getId().equals(currentUserId)) {
            throw new DocumentException("非法越权访问：您无权查看他人的文档数据");
        }
    }

    // ---------------- 内部工具方法 ----------------

    /**
     * (已弃用）
     * 将 Entity 转化为 VO，屏蔽物理路径，扁平化用户信息
     */

//    @Deprecated
//    private DocumentVO convertToVO(Document document) {
//        if (document == null) return null;
//
//        String uploaderName = document.getUploader() != null ? document.getUploader().getUsername() : "未知";
//
//        return new DocumentVO(
//                document.getId(),
//                document.getName(),
//                document.getOriginalName(),
//                document.getFileType(),
//                document.getFileSize(),
//                document.getStatus(),
//                document.getSummary(),
//                document.getErrorMsg(),
//                uploaderName,
//                document.getAllowedRoles(),
//                document.getCreateTime()
//        );
//    }
}