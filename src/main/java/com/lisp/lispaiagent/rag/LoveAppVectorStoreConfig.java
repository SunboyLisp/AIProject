package com.lisp.lispaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 配置类，用于定义和初始化与向量存储相关的 Bean。
 * 主要作用是创建一个基于 Markdown 文档内容的 VectorStore 实例，
 * 用于后续的相似性搜索或 AI 模型交互。
 */
@Configuration
public class LoveAppVectorStoreConfig {

    /**
     * 注入文档加载器实例，用于从 classpath 加载 Markdown 格式的文档数据。
     */
    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    /**
     *
     * @param dashscopeEmbeddingModel 提供文本到向量转换能力的 Embedding 模型
     * @return 构建完成并已加载文档数据的 VectorStore 实例
     */
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        // 使用 DashScope Embedding 模型构建 SimpleVectorStore 实例
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();

        // 调用文档加载器方法，获取所有 Markdown 文件解析后的 Document 对象列表
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();

        // 将加载的文档添加到 VectorStore 中，建立向量索引以支持语义检索
        simpleVectorStore.add(documents);

        // 返回配置完成的 VectorStore 实例
        return simpleVectorStore;
    }
}
