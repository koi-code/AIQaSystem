package com.yiwilee.aiqasystem.service.impl;

import com.github.junrar.exception.RarException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yiwilee.aiqasystem.config.MilvusConfig;
import com.yiwilee.aiqasystem.exception.RagException;
import com.yiwilee.aiqasystem.service.VectorService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private final MilvusClientV2 milvusClient;
    private final MilvusConfig milvusConfig;
    private final Gson gson = new Gson();

    // 表结构字段定义 (Metadata Schema)
    private static final String FIELD_ID = "id";                   // Milvus 自带自增主键
    private static final String FIELD_CHUNK_ID = "chunk_id";       // 关联 MySQL 的 DocumentChunk ID
    private static final String FIELD_TYPE = "type";               // 数据类型 (chunk)
    private static final String FIELD_ALLOWED_ROLES = "allowed_roles"; // 允许访问的角色 (JSON数组)
    private static final String FIELD_VECTOR = "vector";           // 向量数据本身

    @PostConstruct
    public void init() {
        try {
            initCollection();
            log.info("Milvus 向量集合初始化成功: {}", milvusConfig.getCollectionName());
        } catch (Exception e) {
            log.error("Milvus 连接失败，请检查 Docker 服务是否启动，错误信息: {}", e.getMessage());
        }
    }

    @Override
    public void initCollection() {
        String collectionName = milvusConfig.getCollectionName();

        Boolean hasCollection = milvusClient.hasCollection(
                HasCollectionReq.builder().collectionName(collectionName).build()
        );

        if (hasCollection) {
            milvusClient.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
            return;
        }

        log.info("集合 {} 不存在，正在创建 Schema 和索引...", collectionName);

        // 创建 Schema
        CreateCollectionReq.CollectionSchema schema = MilvusClientV2.CreateSchema();

        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_ID).dataType(DataType.Int64).isPrimaryKey(true).autoID(true).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_CHUNK_ID).dataType(DataType.Int64).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_TYPE).dataType(DataType.VarChar).maxLength(50).build());
        // 使用 JSON 类型存储权限列表，支撑底层越权拦截
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_ALLOWED_ROLES).dataType(DataType.JSON).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_VECTOR).dataType(DataType.FloatVector).dimension(milvusConfig.getDimension()).build());

        // 定义 HNSW 高性能索引 (余弦相似度)
        IndexParam indexParam = IndexParam.builder()
                .fieldName(FIELD_VECTOR)
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of("M", "16", "efConstruction", "200"))
                .build();

        // 创建并加载集合到内存
        milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(Collections.singletonList(indexParam))
                .build());

        milvusClient.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
    }

    @Override
    public String insertVector(Long businessId, float[] vector, String type, List<String> allowedRoles) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(FIELD_CHUNK_ID, businessId);
        jsonObject.addProperty(FIELD_TYPE, type);

        // 序列化角色列表存入 JSON 字段，打上“思想钢印”
        JsonArray rolesArray = gson.toJsonTree(allowedRoles).getAsJsonArray();
        jsonObject.add(FIELD_ALLOWED_ROLES, rolesArray);

        JsonArray vectorArray = new JsonArray();
        for (float v : vector) {
            vectorArray.add(v);
        }
        jsonObject.add(FIELD_VECTOR, vectorArray);

        InsertResp resp = milvusClient.insert(InsertReq.builder()
                .collectionName(milvusConfig.getCollectionName())
                .data(Collections.singletonList(jsonObject))
                .build());

        return resp.getInsertCnt() > 0 ? "SUCCESS" : null;
    }

    @Override
    public void deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }

        // 将 List<Long> [1, 2, 3] 转换为字符串 "[1, 2, 3]"，刚好符合 Milvus 的 in 语法
        String idsStr = chunkIds.toString();
        String filterExpr = FIELD_CHUNK_ID + " in " + idsStr;

        log.info("准备批量清理 Milvus 向量, 过滤条件: {}", filterExpr);

        try {
            milvusClient.delete(DeleteReq.builder()
                    .collectionName(milvusConfig.getCollectionName())
                    .filter(filterExpr)
                    .build());
            log.info("Milvus 清理成功");
        } catch (Exception e) {
            log.error("Milvus 清理失败，发生未知错误：{}", e.getMessage());
            throw new RagException("Milvus 向量数据清理发生错误：" + e.getMessage());
        }
    }

    @Override
    public List<VectorSearchResult> searchWithRoles(float[] queryVector, int topK, String type, List<String> userRoles) {
        // 安全底线：没有角色的用户，什么都搜不出来
        if (userRoles == null || userRoles.isEmpty()) {
            log.warn("拦截了一次无角色权限的向量检索请求");
            return Collections.emptyList();
        }

        // ========================================================
        // 动态拼接 Milvus JSON 过滤表达式 (防水平越权核心)
        // 效果：type == "chunk" && JSON_CONTAINS_ANY(allowed_roles, ["ROLE_A","ROLE_B"])
        // ========================================================
        String rolesJson = gson.toJson(userRoles);
        String filterExpr = String.format("%s == \"%s\" && JSON_CONTAINS_ANY(%s, %s)",
                FIELD_TYPE, type, FIELD_ALLOWED_ROLES, rolesJson);

        log.info("执行 Milvus 高维检索, 条件: {}", filterExpr);

        SearchReq searchReq = SearchReq.builder()
                .collectionName(milvusConfig.getCollectionName())
                .outputFields(Arrays.asList(FIELD_CHUNK_ID, FIELD_TYPE))
                .data(Collections.singletonList(new FloatVec(queryVector)))
                .annsField(FIELD_VECTOR)
                .filter(filterExpr) // 强制注入权限拦截！
                .limit(topK)
                .build();

        SearchResp resp = milvusClient.search(searchReq);
        List<SearchResp.SearchResult> results = resp.getSearchResults().stream().flatMap(List::stream).toList();

        List<VectorSearchResult> parsedResults = new ArrayList<>();
        for (SearchResp.SearchResult r : results) {
            Map<String, Object> entity = r.getEntity();
            if (entity != null && entity.containsKey(FIELD_CHUNK_ID)) {
                // 安全转型：防止不同版本 SDK 返回类型不一致 (Integer/Long/String)
                Long chunkId = Long.parseLong(String.valueOf(entity.get(FIELD_CHUNK_ID)));
                String resType = String.valueOf(entity.get(FIELD_TYPE));
                parsedResults.add(new VectorSearchResult(chunkId, resType, r.getScore()));
            }
        }

        return parsedResults;
    }
}