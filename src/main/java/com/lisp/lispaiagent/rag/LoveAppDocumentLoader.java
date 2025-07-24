package com.lisp.lispaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * LoveAppDocumentLoader 类负责加载 Markdown 文档，
 * 并将其转换为 Spring AI 的 Document 对象列表。
 */
@Component
@Slf4j
class LoveAppDocumentLoader {

    /**
     * 资源模式解析器，用于根据指定的模式获取资源。
     */
    private final ResourcePatternResolver resourcePatternResolver;

    /**
     * 构造函数，注入 ResourcePatternResolver 实例。
     *
     * @param resourcePatternResolver 资源模式解析器实例
     */
    LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
 * 加载 classpath 下指定目录中的所有 Markdown 文档，
 * 并将其转换为 Spring AI 的 Document 对象列表。
 *
 * @return 包含所有 Markdown 文档内容的 Document 对象列表
 */
public List<Document> loadMarkdowns() {
    // 初始化一个空的 Document 对象列表，用于存储所有 Markdown 文档转换后的 Document 对象
    List<Document> allDocuments = new ArrayList<>();
    try {
        // 获取 classpath 下指定目录中的所有资源
        Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
        // 遍历每个资源，即每个 Markdown 文件
        for (Resource resource : resources) {
            // 获取当前文件的文件名
            String fileName = resource.getFilename();
            // 提取文档倒数第 3 和第 2 个字作为标签
            String status = fileName.substring(fileName.length() - 6, fileName.length() - 4);
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", fileName)
                    .withAdditionalMetadata("status", status)
                    .build();
            // 创建 MarkdownDocumentReader 对象，用于读取和解析 Markdown 文件
            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
            // 将解析后的 Document 对象添加到列表中
            allDocuments.addAll(reader.get());
        }
    } catch (IOException e) {
        // 如果发生 IO 异常，记录错误日志
        log.error("Markdown 文档加载失败", e);
    }
    // 返回包含所有 Document 对象的列表
    return allDocuments;
}

}
